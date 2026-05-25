package net.simplx.philter;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import static net.simplx.philter.FilterBlockEntity.EXAMPLES_COUNT;
import static net.simplx.philter.PhilterMod.FILTER_SCREEN_HANDLER;

public class FilterScreenHandler extends AbstractContainerMenu {

  static final int EXAMPLES_GRID_COLS = 4;
  static final int EXAMPLES_GRID_ROWS = 4;
  static final int EXAMPLES_GRID_X = 223;
  static final int EXAMPLES_GRID_Y = 51;

  private static final int SLOT_COUNT = 5;

  private final FilterDesc filterDesc;
  private final Container inventory;

  final BlockPos pos;
  final Direction facing;
  Direction userFacingDir;
  Direction filter;
  FilterSlot[] filterSlots;

  static {
    //noinspection ConstantConditions
    if (EXAMPLES_GRID_COLS * EXAMPLES_GRID_ROWS != EXAMPLES_COUNT) {
      throw new IllegalStateException("Size mismatch! " + (EXAMPLES_GRID_COLS * EXAMPLES_GRID_ROWS) + " != " + SLOT_COUNT);
    }
  }

  public FilterScreenHandler(int syncId, Inventory playerInventory, FilterData data) {
    this(syncId, playerInventory, new SimpleContainer(SLOT_COUNT + EXAMPLES_COUNT), data);
    userFacingDir = data.userFacing();
  }

  public FilterScreenHandler(int syncId, Inventory playerInventory, Container inventory, FilterData data) {
    super(FILTER_SCREEN_HANDLER, syncId);
    this.inventory = inventory;
    checkContainerSize(inventory, SLOT_COUNT + EXAMPLES_COUNT);
    inventory.startOpen(playerInventory.player);
    this.filterDesc = data.desc();
    this.pos = data.pos();
    this.facing = data.facing();
    this.filter = data.filter();
    setup(playerInventory, inventory, data.userFacing() == null);
  }

  private void setup(Inventory playerInventory, Container inventory, boolean onServer) {
    filterSlots = new FilterSlot[EXAMPLES_COUNT];
    int i;
    for (i = 0; i < 5; ++i) {
      addSlot(new Slot(inventory, i, 44 + i * 18, 20));
    }
    int slotNum = 0;
    @SuppressWarnings("UnnecessaryLocalVariable") boolean alwaysEnabled = onServer;
    for (int slotRow = 0; slotRow < EXAMPLES_GRID_ROWS; slotRow++) {
      for (int slotCol = 0; slotCol < EXAMPLES_GRID_COLS; slotCol++) {
        FilterSlot slot = new FilterSlot(this.inventory, SLOT_COUNT + slotNum, EXAMPLES_GRID_X + slotCol * 18,
            EXAMPLES_GRID_Y + 18 * slotRow, alwaysEnabled);
        filterSlots[slotNum++] = slot;
        addSlot(slot);
      }
    }
    for (i = 0; i < 3; ++i) {
      for (int k = 0; k < 9; ++k) {
        addSlot(new Slot(playerInventory, k + i * 9 + 9, 8 + k * 18, i * 18 + 51));
      }
    }
    for (i = 0; i < 9; ++i) {
      addSlot(new Slot(playerInventory, i, 8 + i * 18, 109));
    }
  }

  @Override
  public boolean stillValid(Player player) {
    return inventory.stillValid(player);
  }

  @Override
  public void removed(Player player) {
    super.removed(player);
    inventory.stopOpen(player);
  }

  @Override
  public ItemStack quickMoveStack(Player player, int slotNum) {
    Slot slot = slots.get(slotNum);
    if (!slot.hasItem()) {
      return ItemStack.EMPTY;
    }

    ItemStack slotStack = slot.getItem();
    ItemStack retVal = slotStack.copy();
    int playerStart = SLOT_COUNT + EXAMPLES_COUNT;
    int playerEnd = playerStart + player.getInventory().getContainerSize();
    if (slotNum >= playerStart && slotNum < playerEnd) {
      if (!moveItemStackTo(slotStack, 0, SLOT_COUNT, false)) {
        return ItemStack.EMPTY;
      }
    } else {
      if (!moveItemStackTo(slotStack, playerStart, playerEnd, true)) {
        return ItemStack.EMPTY;
      }
    }
    if (slotStack.isEmpty()) {
      slot.set(ItemStack.EMPTY);
    } else {
      slot.setChanged();
    }
    return retVal;
  }

  public FilterDesc getFilterDesc() {
    return filterDesc;
  }

  public BlockPos getPos() {
    return pos;
  }
}
