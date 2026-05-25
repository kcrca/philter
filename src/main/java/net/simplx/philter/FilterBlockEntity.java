package net.simplx.philter;

import com.google.common.collect.ImmutableList;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static net.simplx.philter.FilterBlock.*;

/**
 * This block is effectively a hopper, but neither {@link HopperBlock} nor {@link HopperBlockEntity} are designed for
 * subclasses. So this is a mash-up of forced inheritance (via access-widener) and copies where needed.
 */
@SuppressWarnings({"SameParameterValue", "unused"})
public class FilterBlockEntity extends HopperBlockEntity implements WorldlyContainer,
    ExtendedMenuProvider<FilterData> {

  static final int EXAMPLES_COUNT = 16;
  static final int EXAMPLES_START = HOPPER_CONTAINER_SIZE;
  static final int EXAMPLES_END = EXAMPLES_START + EXAMPLES_COUNT;

  private static final int[] INVENTORY_INDEXES = new int[]{0, 1, 2, 3, 4};

  private NonNullList<ItemStack> filterItems = NonNullList.withSize(HOPPER_CONTAINER_SIZE + EXAMPLES_COUNT, ItemStack.EMPTY);

  private FilterDesc desc;
  private FilterMatches filterMatches;
  private int flicker;
  private Direction userFacingDir;

  @Override
  public boolean isValidBlockState(BlockState state) {
    return PhilterMod.FILTER_BLOCK_ENTITY != null
        ? PhilterMod.FILTER_BLOCK_ENTITY.isValid(state)
        : state.getBlock() == PhilterMod.FILTER_BLOCK;
  }

  protected FilterBlockEntity(BlockPos pos, BlockState state) {
    super(pos, state);
    type = PhilterMod.FILTER_BLOCK_ENTITY;
    desc = new FilterDesc(FilterMode.SAME_AS, ImmutableList.of(), false);
    filterMatches = new FilterMatches(ImmutableList.of());
    flicker = 0;
  }

  @Override
  public int getContainerSize() {
    return filterItems.size();
  }

  @Override
  protected NonNullList<ItemStack> getItems() {
    return filterItems;
  }

  @Override
  protected void setItems(NonNullList<ItemStack> items) {
    filterItems = items;
  }

  public static void updateEntity(Player player, FilterData data) {
    FilterDesc filterDesc = data.desc();
    BlockPos pos = data.pos();
    var rawEntity = player.level().getBlockEntity(pos);
    if (rawEntity instanceof FilterBlockEntity) {
      ((FilterBlockEntity) rawEntity).setFilterDesc(filterDesc);
      Direction newFilterDir = data.filter();
      if (rawEntity.getBlockState().getValue(FILTER) != newFilterDir) {
        player.level().setBlockAndUpdate(pos, rawEntity.getBlockState().setValue(FILTER, newFilterDir));
      }
      rawEntity.setChanged();
    }
  }

  @Override
  public boolean isEmpty() {
    for (ItemStack itemStack : filterItems.subList(0, HOPPER_CONTAINER_SIZE)) {
      if (!itemStack.isEmpty()) {
        return false;
      }
    }
    return true;
  }

  @Override
  protected boolean inventoryFull() {
    for (int i = 0; i < HOPPER_CONTAINER_SIZE; i++) {
      ItemStack itemStack = filterItems.get(i);
      if (!itemStack.isEmpty() && itemStack.getCount() == itemStack.getMaxStackSize()) {
        continue;
      }
      return false;
    }
    return true;
  }

  @Override
  protected void loadAdditional(ValueInput input) {
    super.loadAdditional(input);
    desc = new FilterDesc(input);
  }

  @Override
  protected void saveAdditional(ValueOutput output) {
    super.saveAdditional(output);
    desc.write(output);
  }

  @Override
  protected Component getDefaultName() {
    return Component.translatable("philter.filter.name");
  }

  public static void serverTick(Level level, BlockPos pos, BlockState state, FilterBlockEntity blockEntity) {
    blockEntity.doServerTick(level, pos, state);
  }

  private void doServerTick(Level level, BlockPos pos, BlockState state) {
    cooldownTime--;
    tickedGameTime = level.getGameTime();
    if (!isOnCooldown()) {
      setCooldown(0);
      boolean changed = HopperBlockEntity.suckInItems(level, this);
      changed |= insertAndExtract(level, pos, state);
      if (changed) {
        setCooldown(8);
        setChanged();
      }
    }
    if (flicker > 0) {
      --flicker;
      int newState = flicker > 0 ? 1 : 0;
      int curState = state.getValue(FILTERED);
      if (newState != curState) {
        level.setBlock(pos, state.setValue(FILTERED, newState), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
      }
    }
  }

  private boolean insertAndExtract(Level level, BlockPos pos, BlockState state) {
    if (level.isClientSide() || !state.getValue(HopperBlock.ENABLED) || isEmpty()) {
      return false;
    }

    Container facingOut = getContainerAt(level, pos.relative(state.getValue(FACING)));
    Direction facingSide = state.getValue(FACING).getOpposite();
    Container filterOut = getContainerAt(level, pos.relative(state.getValue(FILTER)));
    Direction filterSide = state.getValue(FILTER).getOpposite();
    if (facingOut == null && filterOut == null) {
      return false;
    }

    for (int i = 0; i < HOPPER_CONTAINER_SIZE; i++) {
      ItemStack stack = getItem(i);
      if (stack.isEmpty()) {
        continue;
      }
      ItemStack toTransfer = stack.copy().split(1);
      boolean moved = false;
      if (filterOut != null && inFilter(stack, filterOut)) {
        moved = tryTransfer(filterOut, filterSide, stack, toTransfer);
        if (moved) {
          flicker = 8;
        }
      }
      if (!moved && facingOut != null) {
        moved = tryTransfer(facingOut, facingSide, stack, toTransfer);
      }
      if (moved) {
        return true;
      }
    }
    return false;
  }

  private boolean tryTransfer(Container out, Direction side, ItemStack stack, ItemStack toTransfer) {
    ItemStack after = HopperBlockEntity.addItem(this, out, toTransfer, side);
    if (after.isEmpty()) {
      stack.shrink(1);
      return true;
    }
    return false;
  }

  private boolean inFilter(ItemStack hopperStack, Container targetInv) {
    if (hopperStack.getCount() == 0) {
      return false;
    }
    return switch (desc.mode) {
      case NONE -> false;
      case SAME_AS -> filterSameAs(hopperStack, targetInv);
      case MATCHES -> filterMatches(hopperStack);
    };
  }

  private boolean filterSameAs(ItemStack item, Container targetInv) {
    List<ItemStack> examples = getExamples(targetInv);
    if (examples == null) return false;
    for (ItemStack invStack : examples) {
      if (desc.exact) {
        if (canMergeItems(invStack, item)) {
          return true;
        }
      } else {
        if (ItemStack.isSameItem(invStack, item)) {
          return true;
        }
      }
    }
    return false;
  }

  @Nullable
  private List<ItemStack> getExamples(Container targetInv) {
    List<ItemStack> examples = new ArrayList<>();
    for (int i = EXAMPLES_START; i < EXAMPLES_END; i++) {
      ItemStack itemStack = filterItems.get(i);
      if (!itemStack.isEmpty()) {
        examples.add(itemStack);
      }
    }
    if (examples.isEmpty()) {
      if (targetInv == null) {
        return null;
      }
      for (int i = 0; i < targetInv.getContainerSize(); i++) {
        ItemStack itemStack = targetInv.getItem(i);
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
  public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
    return new FilterScreenHandler(syncId, playerInventory, this, new FilterData(
        desc, worldPosition, getBlockState().getValue(FACING),
        getBlockState().getValue(FILTER), null));
  }

  @Override
  public FilterData getScreenOpeningData(ServerPlayer player) {
    BlockState state = player.level().getBlockState(worldPosition);
    return new FilterData(desc, worldPosition, state.getValue(FACING), state.getValue(FILTER), userFacingDir);
  }

  @Override
  public Component getDisplayName() {
    return getDefaultName();
  }

  public void setFilterDesc(FilterDesc desc) {
    this.desc = desc;
    setChanged();
  }

  public void setActionDir(Direction userFacingDir) {
    this.userFacingDir = userFacingDir;
  }

  @Override
  public int[] getSlotsForFace(Direction side) {
    return INVENTORY_INDEXES;
  }

  @Override
  public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
    return true;
  }

  @Override
  public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
    return true;
  }

  public static void onEntityCollided(Level level, BlockPos pos, BlockState state, Entity entity,
                                      HopperBlockEntity blockEntity) {
  }
}
