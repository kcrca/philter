package net.simplx.philter;

import com.google.common.collect.ImmutableList;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HopperBlock;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import static net.minecraft.util.math.Direction.UP;
import static net.simplx.philter.FilterBlock.*;

/**
 * This block is effectively a hopper, but neither {@link HopperBlock} nor {@link HopperBlockEntity} are designed for
 * subclasses. So this is a mash-up of forced inheritance (via access-widener) and copies where needed. The alternative
 * is to simply copy the entire hopper entity class and tweak it. This way, at least what <em>can</em> be inherited is
 * inherited.
 *
 * This is only to be able to re-write the {@link HopperBlockEntity#insertAndExtract} method to check the filter before
 * doing any move. The static {@link #serverTick} here simply invokes {@link #doServerTick} as an instance method, which
 * mirrors the static {@link HopperBlockEntity#serverTick} (non-statically) and so on until we get to
 * {@link #insertAndExtract}. Everything below that we just invoke the superclass method.
 */
@SuppressWarnings("SameParameterValue")
public class FilterBlockEntity extends HopperBlockEntity implements SidedInventory, ExtendedScreenHandlerFactory {

  static final int EXAMPLES_COUNT = 16;

  private static final int[] HOPPER_SLOTS;
  private static final int[] EXAMPLE_SLOTS;

  static {
    HOPPER_SLOTS = new int[INVENTORY_SIZE];
    for (int i = 0; i < INVENTORY_SIZE; i++) {
      HOPPER_SLOTS[i] = i;
    }
    EXAMPLE_SLOTS = new int[EXAMPLES_COUNT];
    for (int i = 0; i < EXAMPLES_COUNT; i++) {
      EXAMPLE_SLOTS[i] = i + INVENTORY_SIZE;
    }
  }

  private FilterDesc desc;
  private FilterMatches filterMatches;
  private int flicker;
  private Direction userFacingDir;

  protected FilterBlockEntity(BlockPos pos, BlockState state) {
    super(pos, state);
    setInvStackList(DefaultedList.ofSize(INVENTORY_SIZE + EXAMPLES_COUNT, ItemStack.EMPTY));
    type = PhilterMod.FILTER_BLOCK_ENTITY;
    desc = new FilterDesc(FilterMode.SAME_AS, ImmutableList.of(), false);
    filterMatches = new FilterMatches(ImmutableList.of());
    flicker = 0;
  }

  public static void updateEntity(PlayerEntity player, PacketByteBuf buf) {
    FilterDesc filterDesc = new FilterDesc(buf);
    BlockPos pos = buf.readBlockPos();
    var rawEntity = player.getWorld().getBlockEntity(pos);
    if (rawEntity instanceof FilterBlockEntity) {
      try {
        ((FilterBlockEntity) rawEntity).setFilterDesc(filterDesc);
        buf.readEnumConstant(Direction.class);
        Direction newFilterDir = buf.readEnumConstant(Direction.class);
        if (rawEntity.getCachedState().get(FILTER) != newFilterDir) {
          player.world.setBlockState(pos, rawEntity.getCachedState().with(FILTER, newFilterDir));
        }
        rawEntity.markDirty();
      } finally {
        buf.release();
      }
    }
  }

  @Override
  public boolean isEmpty() {
    this.checkLootInteraction(null);
    return this.getInvStackList().subList(0, INVENTORY_SIZE).stream().allMatch(ItemStack::isEmpty);
  }

  /**
   * Overridden so it only examines the hopper's part of the inventory.j
   */
  public boolean isFull() {
    DefaultedList<ItemStack> invStackList = getInvStackList();
    for (int i = 0; i < INVENTORY_SIZE; i++) {
      ItemStack itemStack = invStackList.get(i);
      if (!itemStack.isEmpty() && itemStack.getCount() == itemStack.getMaxCount()) {
        continue;
      }
      return false;
    }
    return true;
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

  public static void serverTick(World world, BlockPos pos, BlockState state, FilterBlockEntity blockEntity) {
    blockEntity.doServerTick(world, pos, state);
  }

  private void doServerTick(World world, BlockPos pos, BlockState state) {
    transferCooldown--;
    lastTickTime = world.getTime();
    if (!needsCooldown()) {
      setTransferCooldown(0);
      insertAndExtract(world, pos, state, () -> extract(world, this));
    }
    if (flicker > 0) {
      --flicker;
      int newState = flicker > 0 ? 1 : 0;
      int curState = state.get(FILTERED);
      if (newState != curState) {
        world.setBlockState(pos, state.with(FILTERED, newState), Block.NOTIFY_LISTENERS);
      }
    }
  }

  @SuppressWarnings("UnusedReturnValue")
  private boolean insertAndExtract(World world, BlockPos pos, BlockState state, BooleanSupplier booleanSupplier) {
    if (world.isClient) {
      return false;
    }
    if (!needsCooldown() && state.get(HopperBlock.ENABLED)) {
      boolean bl = false;
      if (!isEmpty()) {
        var filterState = state.with(FACING, state.get(FILTER));
        SimpleInventory tmpInventory = new SimpleInventory(size());
        for (int i = 0; i < size(); i++) {
          if (inFilter(getStack(i), world, pos, state)) {
            tmpInventory.setStack(i, getStack(i));
            world.setBlockState(pos, state.with(FILTERED, 1), Block.NOTIFY_LISTENERS);
            flicker = 8;
            break;
          }
        }
        if (!tmpInventory.isEmpty() && insert(world, pos, filterState, this)) {
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

  public void onEntityCollided(World world, BlockPos pos, BlockState state, Entity entity) {
    if (entity instanceof ItemEntity && VoxelShapes.matchesAnywhere(
        VoxelShapes.cuboid(entity.getBoundingBox().offset(-pos.getX(), -pos.getY(), -pos.getZ())), getInputAreaShape(),
        BooleanBiFunction.AND)) {
      insertAndExtract(world, pos, state, () -> HopperBlockEntity.extract(this, (ItemEntity) entity));
    }
  }

  private boolean inFilter(ItemStack hopperStack, World world, BlockPos pos, BlockState state) {
    if (hopperStack.getCount() == 0) {
      return false;
    }
    return switch (desc.mode) {
      case NONE -> false;
      case SAME_AS -> filterSameAs(hopperStack, world, pos, state);
      case MATCHES -> filterMatches(hopperStack);
    };
  }

  private boolean filterSameAs(ItemStack item, World world, BlockPos pos, BlockState state) {
    List<ItemStack> examples = getExamples(world, pos, state);
    if (examples == null) return false;
    for (ItemStack invStack : examples) {
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

  @Nullable
  private List<ItemStack> getExamples(World world, BlockPos pos, BlockState state) {
    List<ItemStack> examples = new ArrayList<>();
    DefaultedList<ItemStack> exampleInv = getInvStackList();
    for (int i = INVENTORY_SIZE; i < INVENTORY_SIZE + EXAMPLES_COUNT; i++) {
      ItemStack itemStack = exampleInv.get(i);
      if (!itemStack.isEmpty()) {
        examples.add(itemStack);
      }
    }
    if (exampleInv.isEmpty()) {
      Direction direction = state.get(FILTER);
      Inventory inventory = getInventoryAt(world, pos.offset(direction));
      if (inventory == null) {
        return null;
      }
      for (int i = 0; i < inventory.size(); i++) {
        ItemStack itemStack = inventory.getStack(i);
        if (!itemStack.isEmpty()) {
          examples.add(itemStack);
        }
      }
    }
    return examples;
  }

  private boolean filterMatches(ItemStack item) {
    if (!filterMatches.input.equals(desc.matches)) {
      filterMatches = new FilterMatches(desc.matches);
    }
    return desc.matchAll ? filterMatches.matchAll(item, desc.exact, true) : filterMatches.matchAny(item, desc.exact,
        false);
  }

  @Override
  protected ScreenHandler createScreenHandler(int syncId, PlayerInventory playerInventory) {
    return new FilterScreenHandler(syncId, playerInventory, this, desc, pos, getCachedState().get(FACING),
        getCachedState().get(FILTER));
  }

  @Override
  public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
    BlockState state = player.getWorld().getBlockState(pos);
    Direction facing = state.get(FACING);
    Direction filter = state.get(FILTER);
    desc.write(buf, pos, facing, filter);
    buf.writeEnumConstant(userFacingDir);
  }

  public void setFilterDesc(FilterDesc desc) {
    this.desc = desc;
    markDirty();
  }

  public void setActionDir(Direction userFacingDir) {

    this.userFacingDir = userFacingDir;
  }

  @Override
  public int[] getAvailableSlots(Direction side) {
    return side == UP ? EXAMPLE_SLOTS : HOPPER_SLOTS;
  }

  @Override
  public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
    return dir != UP;
  }

  @Override
  public boolean canExtract(int slot, ItemStack stack, Direction dir) {
    return dir != UP;
  }
}
