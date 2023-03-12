package net.simplx.philter;

import com.google.common.collect.ImmutableList;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HopperBlock;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static net.simplx.philter.FilterBlock.*;

/**
 * This block is effectively a hopper, but neither {@link HopperBlock} nor {@link HopperBlockEntity} are designed for
 * subclasses. So this is a mash-up of forced inheritance (via access-widener) and copies where needed. The alternative
 * is to simply copy the entire hopper entity class and tweak it. This way, at least what <em>can</em> be inherited is
 * inherited.
 * <p>
 * This is only to be able to re-write the {@link HopperBlockEntity#insertAndExtract} method to check the filter before
 * doing any move. The static {@link #serverTick} here simply invokes {@link #doServerTick} as an instance method, which
 * mirrors the static {@link HopperBlockEntity#serverTick} (non-statically) and so on until we get to
 * {@link #insertAndExtract}. Everything below that we just invoke the superclass method.
 */
@SuppressWarnings("SameParameterValue")
public class FilterBlockEntity extends HopperBlockEntity implements SidedInventory, ExtendedScreenHandlerFactory {

  static final int EXAMPLES_COUNT = 16;
  static final int EXAMPLES_START = INVENTORY_SIZE;
  static final int EXAMPLES_END = EXAMPLES_START + EXAMPLES_COUNT;

  private static final int[] INVENTORY_INDEXES = new int[]{0, 1, 2, 3, 4};

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
    checkLootInteraction(null);
    return getInvStackList().subList(0, INVENTORY_SIZE).stream().allMatch(ItemStack::isEmpty);
  }

  /**
   * Overridden so it only examines the hopper's part of the inventory.j
   */
  @Override
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
      insertAndExtract(world, pos, state);
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

  private void insertAndExtract(World world, BlockPos pos, BlockState state) {
    if (world.isClient) {
      return;
    }
    if (needsCooldown() || !state.get(HopperBlock.ENABLED) || isEmpty()) {
      return;
    }

    Inventory facingOut = getInventoryAt(world, pos.offset(state.get(FACING)));
    Direction facingSide = state.get(FACING).getOpposite();
    Inventory filterOut = getInventoryAt(world, pos.offset(state.get(FILTER)));
    Direction filterSide = state.get(FILTER).getOpposite();
    if (facingOut == null && filterOut == null) {
      return;
    }

    boolean changed = false;
    for (int i = 0; i < INVENTORY_SIZE; i++) {
      ItemStack stack = getStack(i);
      if (stack.isEmpty()) {
        continue;
      }
      ItemStack toTransfer = stack.copy().split(1);
      if (filterOut != null && inFilter(stack, filterOut)) {
        changed = tryTransfer(filterOut, filterSide, stack, toTransfer);
        if (changed) {
          flicker = 8;
        }
      }
      if (!changed && facingOut != null) {
        changed = tryTransfer(facingOut, facingSide, stack, toTransfer);
      }
      if (changed) {
        break;
      }
    }
    if (changed) {
      setTransferCooldown(8);
      HopperBlockEntity.markDirty(world, pos, state);
    }
  }

  private boolean tryTransfer(Inventory out, Direction filterSide, ItemStack stack, ItemStack toTransfer) {
    ItemStack after = HopperBlockEntity.transfer(this, out, toTransfer, filterSide);
    if (after.isEmpty()) {
      stack.decrement(1);
      return true;
    }
    return false;
  }

  private boolean inFilter(ItemStack hopperStack, Inventory targetInv) {
    if (hopperStack.getCount() == 0) {
      return false;
    }
    return switch (desc.mode) {
      case NONE -> false;
      case SAME_AS -> filterSameAs(hopperStack, targetInv);
      case MATCHES -> filterMatches(hopperStack);
    };
  }

  private boolean filterSameAs(ItemStack item, Inventory targetInv) {
    List<ItemStack> examples = getExamples(targetInv);
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
  private List<ItemStack> getExamples(Inventory targetInv) {
    List<ItemStack> examples = new ArrayList<>();
    DefaultedList<ItemStack> exampleInv = getInvStackList();
    for (int i = EXAMPLES_START; i < EXAMPLES_END; i++) {
      ItemStack itemStack = exampleInv.get(i);
      if (!itemStack.isEmpty()) {
        examples.add(itemStack);
      }
    }
    if (examples.isEmpty()) {
      if (targetInv == null) {
        return null;
      }
      for (int i = 0; i < targetInv.size(); i++) {
        ItemStack itemStack = targetInv.getStack(i);
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
    return desc.matchAll ? filterMatches.matchAll(item, desc.exact, true) : filterMatches.matchAny(item, desc.exact, false);
  }

  @Override
  protected ScreenHandler createScreenHandler(int syncId, PlayerInventory playerInventory) {
    return new FilterScreenHandler(syncId, playerInventory, this, desc, pos, getCachedState().get(FACING), getCachedState().get(FILTER), true);
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
    return INVENTORY_INDEXES;
  }

  @Override
  public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
    return true;
  }

  @Override
  public boolean canExtract(int slot, ItemStack stack, Direction dir) {
    return true;
  }

  /** HopperBlockEntity uses this to suck items out of the world. We don't want to, since we don't pull from above. */
  public static void onEntityCollided(World world, BlockPos pos, BlockState state, Entity entity, HopperBlockEntity blockEntity) {
  }
}
