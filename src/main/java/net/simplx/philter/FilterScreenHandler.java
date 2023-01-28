package net.simplx.philter;

import static net.minecraft.screen.HopperScreenHandler.SLOT_COUNT;
import static net.simplx.philter.FilterBlockEntity.EXAMPLES_COUNT;
import static net.simplx.philter.PhilterMod.FILTER_SCREEN_HANDLER;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class FilterScreenHandler extends ScreenHandler {

  static final int EXAMPLES_GRID_X = 4;
  static final int EXAMPLES_GRID_Y = 4;

  private final FilterDesc filterDesc;
  private final Inventory inventory;

  final BlockPos pos;
  final Direction facing;
  Direction userFacingDir;
  Direction filter;
  FilterSlot[] filterSlots;

  static {
    //noinspection ConstantConditions
    if (EXAMPLES_GRID_X * EXAMPLES_GRID_Y != EXAMPLES_COUNT) {
      throw new IllegalStateException(
          "Size mismatch! " + (EXAMPLES_GRID_X * EXAMPLES_GRID_Y) + " != " + SLOT_COUNT);
    }
  }

  public FilterScreenHandler(int syncId, PlayerInventory playerInventory, PacketByteBuf buf) {
    this(syncId, playerInventory, new SimpleInventory(SLOT_COUNT + EXAMPLES_COUNT),
        new FilterDesc(buf), buf.readBlockPos(), buf.readEnumConstant(Direction.class),
        buf.readEnumConstant(Direction.class));
    userFacingDir = buf.readEnumConstant(Direction.class);
  }

  public FilterScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory,
      FilterDesc filterDesc, BlockPos pos, Direction facing, Direction filter) {
    super(FILTER_SCREEN_HANDLER, syncId);
    this.inventory = inventory;
    checkSize(inventory, SLOT_COUNT + EXAMPLES_COUNT);
    inventory.onOpen(playerInventory.player);
    this.filterDesc = filterDesc;
    filterSlots = new FilterSlot[EXAMPLES_COUNT];
    int i;
    for (i = 0; i < 5; ++i) {
      addSlot(new Slot(inventory, i, 44 + i * 18, 20));
    }
    initializeSlots(200, 80, 18, 18);
    for (i = 0; i < 3; ++i) {
      for (int k = 0; k < 9; ++k) {
        addSlot(new Slot(playerInventory, k + i * 9 + 9, 8 + k * 18, i * 18 + 51));
      }
    }
    for (i = 0; i < 9; ++i) {
      addSlot(new Slot(playerInventory, i, 8 + i * 18, 109));
    }
    this.pos = pos;
    this.facing = facing;
    this.filter = filter;
  }

  void initializeSlots(int x, int y, int slotW, int slotH) {
    int slotNum = 0;
    for (int slotRow = 0; slotRow < EXAMPLES_GRID_Y; slotRow++) {
      for (int slotCol = 0; slotCol < EXAMPLES_GRID_X; slotCol++) {
        FilterSlot slot = new FilterSlot(inventory, SLOT_COUNT + slotNum, x + slotCol * slotW,
            y + slotH * slotRow);
        filterSlots[slotNum++] = slot;
        addSlot(slot);
      }
    }
  }

  @Override
  public boolean canUse(PlayerEntity player) {
    return inventory.canPlayerUse(player);
  }

  @Override
  public void close(PlayerEntity player) {
    super.close(player);
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
    int playerStart = SLOT_COUNT;
    int playerEnd = playerStart + player.getInventory().main.size();
    if (slotNum >= playerStart && slotNum < playerEnd) {
      // Move the player's inventory into the hoopper slots
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
