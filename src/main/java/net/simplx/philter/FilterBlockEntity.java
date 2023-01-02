package net.simplx.philter;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.BooleanSupplier;
import net.minecraft.block.BlockState;
import net.minecraft.block.HopperBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.text.Text;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This implementation is ... suboptimal. This block is effectively a hopper, but neither
 * HopperVBlock nor HopperBlocKEntity are designed for subclasses. So this is a mash-up of forced
 * semi-inheritance and copies where needed. The alternative is to simply copy the entire hopper
 * entity class and tweak it. This way, at least what <em>can</em> be inherited is inherited.
 *
 * This is entirely to be able to re-write the insert() method to check the filter before doing any
 * move. The static serverTick() here simply invokes doServerTick() as a instance method, which
 * mirrors the static HopperBlockState.serverTick() (non-staticly) and so on until we get to
 * insert(). Everything below that we just invoke the superclass method (honestly or dishonestly).
 *
 * This also requires handling entites dropped on top specially because of course it does; see
 * {@link FilterBlockEntity#onEntityCollided}.
 */
public class FilterBlockEntity extends HopperBlockEntity {

  public static final Field TRANSFER_COOLDOWN_F = field("transferCooldown");
  public static final Field LAST_TICK_TIME_F = field("lastTickTime");
  public static final Field INVENTORY_F = field("inventory");
  public static final Field TYPE_F = field(BlockEntity.class, "type");
  public static final Method NEEDS_COOLDOWN_M = method("needsCooldown");
  public static final Method SET_TRANSFER_COOLDOWN_M = method("setTransferCooldown", int.class);
  public static final Method IS_FULL_M = method("isFull");
  public static final Method IS_INVENTORY_FULL_M = method("isInventoryFull", Inventory.class,
      Direction.class);
  public static final Method GET_OUTPUT_INVENTORY_M = method("getOutputInventory", World.class,
      BlockPos.class, BlockState.class);

  protected final Logger logger;


  private static Field field(String name) {
    return field(HopperBlockEntity.class, name);
  }

  private static Field field(Class<?> clz, String name) {
    try {
      var field = clz.getDeclaredField(name);
      field.setAccessible(true);
      return field;
    } catch (NoSuchFieldException e) {
      throw new IllegalStateException(e);
    }
  }

  private static Method method(Class<?> clz, String name, Class<?>... parameterTypes) {
    try {
      var method = clz.getDeclaredMethod(name, parameterTypes);
      method.setAccessible(true);
      return method;
    } catch (NoSuchMethodException e) {
      throw new IllegalStateException(e);
    }
  }

  private static Method method(String name, Class<?>... parameterTypes) {
    return method(HopperBlockEntity.class, name, parameterTypes);
  }

  protected FilterBlockEntity(BlockPos pos, BlockState state) {
    super(pos, state);
    forceSet(TYPE_F, Philter.FILTER_BLOCK_ENTITY);
    this.logger = LoggerFactory.getLogger("philter");
  }

  private Object forceGet(Field field) {
    try {
      return field.get(this);
    } catch (IllegalAccessException e) {
      throw new IllegalStateException(e);
    }
  }

  private void forceSet(Field field, Object value) {
    try {
      field.set(this, value);
    } catch (IllegalAccessException e) {
      throw new IllegalStateException(e);
    }
  }

  private Object forceInvoke(Method method, Object... parameters) {
    try {
      return method.invoke(this, parameters);
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  protected Text getContainerName() {
    return Text.translatable("container.filter");
  }

  public static void serverTick(World world, BlockPos pos, BlockState state,
      FilterBlockEntity blockEntity) {
    blockEntity.doServerTick(world, pos, state);
  }

  private boolean needsCooldown() {
    return (Boolean) forceInvoke(NEEDS_COOLDOWN_M);
  }

  private void setTransferCooldown(int transferCooldown) {
    forceInvoke(SET_TRANSFER_COOLDOWN_M, transferCooldown);
  }

  private boolean isFull() {
    return (Boolean) forceInvoke(IS_FULL_M);
  }

  private Inventory getOutputInventory(World world, BlockPos pos, BlockState state) {
    return (Inventory) forceInvoke(GET_OUTPUT_INVENTORY_M, world, pos, state);
  }

  private boolean isInventoryFull(Inventory inventory, Direction direction) {
    return (Boolean) forceInvoke(IS_INVENTORY_FULL_M, inventory, direction);
  }

  private void doServerTick(World world, BlockPos pos, BlockState state) {
    forceSet(TRANSFER_COOLDOWN_F, (int) forceGet(TRANSFER_COOLDOWN_F) - 1);
    forceSet(LAST_TICK_TIME_F, world.getTime());
    if (!needsCooldown()) {
      setTransferCooldown(0);
      insertAndExtract(world, pos, state, () -> extract(world, this));
    }
  }

  private boolean insertAndExtract(World world, BlockPos pos, BlockState state,
      BooleanSupplier booleanSupplier) {
    if (world.isClient) {
      return false;
    }
    if (!needsCooldown() && state.get(HopperBlock.ENABLED)) {
      boolean bl = false;
      if (!isEmpty()) {
        var filterState = state.with(FilterBlock.FACING, state.get(FilterBlock.FILTER));
        SimpleInventory filterInventory = new SimpleInventory(size());
        for (int i = 0; i < size(); i++) {
          if (inFilter(getStack(i))) {
            filterInventory.setStack(i, getStack(i));
          }
        }
        if (!filterInventory.isEmpty() && insert(world, pos, filterState, this)) {
          bl = true;
        } else {
          bl = insert(world, pos, state, this);
        }
      }
      if (!isFull()) {
        bl |= booleanSupplier.getAsBoolean();
      }
      if (bl) {
        setTransferCooldown(8);
        HopperBlockEntity.markDirty(world, pos, state);
        return true;
      }
    }
    return false;
  }

  private boolean insert(World world, BlockPos pos, BlockState state, Inventory inventory) {
    Inventory inventory2 = getOutputInventory(world, pos, state);
    if (inventory2 == null) {
      return false;
    }
    return insertFacing(state, inventory2, FilterBlock.FACING, inventory);
  }

  private boolean insertFacing(BlockState state, Inventory inventory2,
      DirectionProperty directionProperty, Inventory inventory) {
    Direction direction = state.get(directionProperty).getOpposite();
    if (isInventoryFull(inventory2, direction)) {
      return false;
    }
    for (int i = 0; i < inventory.size(); ++i) {
      if (inventory.getStack(i).isEmpty()) {
        continue;
      }
      ItemStack itemStack = inventory.getStack(i).copy();
      String tstack = itemStack.toString();
      ItemStack itemStack2 = HopperBlockEntity.transfer(inventory, inventory2,
          inventory.removeStack(i, 1), direction);
      if (itemStack2.isEmpty()) {
        inventory2.markDirty();
        logger.debug("transfered " + direction + ": " + tstack);
        return true;
      }
      inventory.setStack(i, itemStack);
    }
    return false;
  }

  public void onEntityCollided(World world, BlockPos pos, BlockState state, Entity entity) {
    if (entity instanceof ItemEntity && VoxelShapes.matchesAnywhere(
        VoxelShapes.cuboid(entity.getBoundingBox().offset(-pos.getX(), -pos.getY(), -pos.getZ())),
        getInputAreaShape(), BooleanBiFunction.AND)) {
      insertAndExtract(world, pos, state,
          () -> HopperBlockEntity.extract(this, (ItemEntity) entity));
    }
  }

  private boolean inFilter(ItemStack hopperStack) {
    if (hopperStack.getCount() == 0) {
      return false;
    }
    Item item = hopperStack.getItem();
    return item.getName().getContent().toString().contains("sand");
  }
}
