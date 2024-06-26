package net.simplx.philter;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import static net.minecraft.screen.HopperScreenHandler.SLOT_COUNT;
import static net.simplx.philter.FilterBlockEntity.EXAMPLES_COUNT;
import static net.simplx.philter.PhilterMod.FILTER_SCREEN_HANDLER;

public class FilterScreenHandler extends ScreenHandler {

  static final int EXAMPLES_GRID_COLS = 4;
  static final int EXAMPLES_GRID_ROWS = 4;
  static final int EXAMPLES_GRID_X = 223;
  static final int EXAMPLES_GRID_Y = 51;

  private final FilterDesc filterDesc;
  private final Inventory inventory;

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

  public FilterScreenHandler(int syncId, PlayerInventory playerInventory, FilterData data) {
    this(syncId, playerInventory, new SimpleInventory(SLOT_COUNT + EXAMPLES_COUNT), data);
    userFacingDir = data.userFacing();
  }

  public FilterScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, FilterData data) {
    super(FILTER_SCREEN_HANDLER, syncId);
    this.inventory = inventory;
    checkSize(inventory, SLOT_COUNT + EXAMPLES_COUNT);
    inventory.onOpen(playerInventory.player);
    this.filterDesc = data.desc();
    this.pos = data.pos();
    this.facing = data.facing();
    this.filter = data.filter();
    setup(playerInventory, inventory, data.userFacing() == null);
  }

  private void setup(PlayerInventory playerInventory, Inventory inventory, boolean onServer) {
    filterSlots = new FilterSlot[EXAMPLES_COUNT];
    int i;
    for (i = 0; i < 5; ++i) {
      addSlot(new Slot(inventory, i, 44 + i * 18, 20));
    }
    int slotNum = 0;
    @SuppressWarnings("UnnecessaryLocalVariable") boolean alwaysEnabled = onServer;
    for (int slotRow = 0; slotRow < EXAMPLES_GRID_ROWS; slotRow++) {
      for (int slotCol = 0; slotCol < EXAMPLES_GRID_COLS; slotCol++) {
        // On the server side these are always enabled.
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
  public boolean canUse(PlayerEntity player) {
    return inventory.canPlayerUse(player);
  }

  @Override
  public void onClosed(PlayerEntity player) {
    super.onClosed(player);
    inventory.onClose(player);
  }

  @Override
  public ItemStack quickMove(PlayerEntity player, int slotNum) {
    Slot slot = slots.get(slotNum);
    if (!slot.hasStack()) {
      return ItemStack.EMPTY;
    }

    ItemStack slotStack = slot.getStack();
    ItemStack retVal = slotStack.copy();
    int playerStart = SLOT_COUNT + EXAMPLES_COUNT;
    int playerEnd = playerStart + player.getInventory().main.size();
    if (slotNum >= playerStart && slotNum < playerEnd) {
      // Move the player's inventory into the hopper slots
      if (!insertItem(slotStack, 0, SLOT_COUNT, false)) {
        return ItemStack.EMPTY;
      }
    } else {
      // Move either of the other type of slots into the player's inventory
      if (!insertItem(slotStack, playerStart, playerEnd, true)) {
        return ItemStack.EMPTY;
      }
    }
    if (slotStack.isEmpty()) {
      slot.setStack(ItemStack.EMPTY);
    } else {
      slot.markDirty();
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
