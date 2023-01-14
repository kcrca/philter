package net.simplx.philter;

import static net.simplx.philter.FilterBlock.FILTER;
import static net.simplx.philter.FilterBlock.FILTERED;

import com.google.common.collect.ImmutableList;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.BooleanSupplier;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HopperBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;

/**
 * This implementation is ... suboptimal. This block is effectively a hopper, but neither
 * HopperVBlock nor HopperBlocKEntity are designed for subclasses. So this is a mash-up of forced
 * semi-inheritance and copies where needed. The alternative is to simply copy the entire hopper
 * entity class and tweak it. This way, at least what <em>can</em> be inherited is inherited.
 *
 * This is entirely to be able to re-write the insert() method to check the filter before doing any
 * move. The static serverTick() here simply invokes doServerTick() as an instance method, which
 * mirrors the static HopperBlockState.serverTick() (non-statically) and so on until we get to
 * insert(). Everything below that we just invoke the superclass method (honestly or dishonestly).
 *
 * This also requires handling entities dropped on top specially because of course it does; see
 * {@link FilterBlockEntity#onEntityCollided}.
 */
@SuppressWarnings("SameParameterValue")
public class FilterBlockEntity extends HopperBlockEntity implements Forcer,
    ExtendedScreenHandlerFactory {

  private static final StaticForcer force = new StaticForcer(HopperBlockEntity.class);
  private static final Field TRANSFER_COOLDOWN_F = force.field("transferCooldown");
  private static final Field LAST_TICK_TIME_F = force.field("lastTickTime");
  private static final Field TYPE_F = Forcer.field(BlockEntity.class, "type");
  private static final Method NEEDS_COOLDOWN_M = force.method("needsCooldown");
  private static final Method SET_TRANSFER_COOLDOWN_M = force.method("setTransferCooldown",
      int.class);
  private static final Method IS_FULL_M = force.method("isFull");
  private static final Method INSERT_M = force.method("insert", World.class, BlockPos.class,
      BlockState.class, Inventory.class);
  private static final Method CAN_MERGE_ITEMS_F = force.method("canMergeItems", ItemStack.class,
      ItemStack.class);

  private FilterDesc desc;
  private FilterMatches filterMatches;
  private int flicker;

  protected FilterBlockEntity(BlockPos pos, BlockState state) {
    super(pos, state);
    forceSet(TYPE_F, PhilterMod.FILTER_BLOCK_ENTITY);
    desc = new FilterDesc(FilterMode.ONLY_SAME, ImmutableList.of(), false);
    filterMatches = new FilterMatches(ImmutableList.of());
    flicker = 0;
  }

  static void updateEntity(ServerPlayerEntity player, PacketByteBuf buf) {
    FilterDesc filterDesc = new FilterDesc(buf);
    BlockPos pos = buf.readBlockPos();
    FilterBlockEntity entity = (FilterBlockEntity) player.getWorld().getBlockEntity(pos);
    if (entity != null) {
      entity.setFilterDesc(filterDesc);
    }
  }

  @Override
  public void readNbt(NbtCompound nbt) {
    super.readNbt(nbt);
    desc = new FilterDesc(nbt);
  }

  @Override
  protected void writeNbt(NbtCompound nbt) {
    super.writeNbt(nbt);
    desc.writeNbt(nbt);
  }

  @Override
  protected Text getContainerName() {
    return Text.translatable("philter.filter.name");
  }

  public static void serverTick(World world, BlockPos pos, BlockState state,
      FilterBlockEntity blockEntity) {
    blockEntity.doServerTick(world, pos, state);
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  private boolean needsCooldown() {
    return (Boolean) forceInvoke(NEEDS_COOLDOWN_M);
  }

  private void setTransferCooldown(int transferCooldown) {
    forceInvoke(SET_TRANSFER_COOLDOWN_M, transferCooldown);
  }

  private boolean isFull() {
    return (Boolean) forceInvoke(IS_FULL_M);
  }

  private boolean canMergeItems(ItemStack stack1, ItemStack stack2) {
    return (Boolean) forceInvoke(CAN_MERGE_ITEMS_F, stack1, stack2);
  }

  private void doServerTick(World world, BlockPos pos, BlockState state) {
    forceSet(TRANSFER_COOLDOWN_F, (int) forceGet(TRANSFER_COOLDOWN_F) - 1);
    forceSet(LAST_TICK_TIME_F, world.getTime());
    if (!needsCooldown()) {
      setTransferCooldown(0);
      insertAndExtract(world, pos, state, () -> extract(world, this));
    }
    if (flicker > 0) {
      --flicker;
      boolean newState = flicker > 0;
      boolean curState = state.get(FILTERED);
      if (newState != curState) {
        world.setBlockState(pos, state.with(FILTERED, newState), Block.NOTIFY_LISTENERS);
      }
    }
  }

  @SuppressWarnings("UnusedReturnValue")
  private boolean insertAndExtract(World world, BlockPos pos, BlockState state,
      BooleanSupplier booleanSupplier) {
    if (world.isClient) {
      return false;
    }
    if (!needsCooldown() && state.get(HopperBlock.ENABLED)) {
      boolean bl = false;
      if (!isEmpty()) {
        var filterState = state.with(FilterBlock.FACING, state.get(FILTER));
        SimpleInventory filterInventory = new SimpleInventory(size());
        for (int i = 0; i < size(); i++) {
          if (inFilter(getStack(i), world, pos, state)) {
            filterInventory.setStack(i, getStack(i));
            world.setBlockState(pos, state.with(FILTERED, true), Block.NOTIFY_LISTENERS);
            flicker = 8;
            break;
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
    return (boolean) forceInvoke(INSERT_M, world, pos, state, inventory);
  }

  public void onEntityCollided(World world, BlockPos pos, BlockState state, Entity entity) {
    if (entity instanceof ItemEntity && VoxelShapes.matchesAnywhere(
        VoxelShapes.cuboid(entity.getBoundingBox().offset(-pos.getX(), -pos.getY(), -pos.getZ())),
        getInputAreaShape(), BooleanBiFunction.AND)) {
      insertAndExtract(world, pos, state,
          () -> HopperBlockEntity.extract(this, (ItemEntity) entity));
    }
  }

  private boolean inFilter(ItemStack hopperStack, World world, BlockPos pos, BlockState state) {
    if (hopperStack.getCount() == 0) {
      return false;
    }
    return switch (desc.mode) {
      case NONE -> false;
      case ONLY_SAME -> filterOnlySame(hopperStack, world, pos, state);
      case MATCHES -> filterMatches(hopperStack);
    };
  }

  private boolean filterOnlySame(ItemStack item, World world, BlockPos pos, BlockState state) {
    Direction direction = state.get(FILTER);
    Inventory inventory = getInventoryAt(world, pos.offset(direction));
    if (inventory == null) {
      return false;
    }
    for (int i = 0; i < inventory.size(); i++) {
      ItemStack invStack = inventory.getStack(i);
      if (desc.exact) {
        if (canMergeItems(invStack, item)) {
          return true;
        }
      } else {
        if (invStack.isItemEqual(item)) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean filterMatches(ItemStack item) {
    if (!filterMatches.input.equals(desc.matches)) {
      filterMatches = new FilterMatches(desc.matches);
    }
    return desc.matchAll ? filterMatches.matchAll(item, desc.exact, true)
        : filterMatches.matchAny(item, desc.exact, false);
  }

  @Override
  protected ScreenHandler createScreenHandler(int syncId, PlayerInventory playerInventory) {
    return new FilterScreenHandler(syncId, playerInventory, this, desc, pos);
  }

  @Override
  public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
    desc.write(buf, pos);
  }

  public void setFilterDesc(FilterDesc desc) {
    this.desc = desc;
    markDirty();
  }
}
