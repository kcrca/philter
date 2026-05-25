package net.simplx.mcguidoc;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class DocScreenHandler extends AbstractContainerMenu {

  public DocScreenHandler(int syncId, Inventory playerInventory, MoodleMod.DummyData stuff) {
    this(syncId, playerInventory, null, stuff);
  }

  public DocScreenHandler(int syncId, Inventory playerInventory, Container inventory, MoodleMod.DummyData stuff) {
    super(MoodleMod.DOC_SCREEN_HANDLER, syncId);
  }

  @Override
  public ItemStack quickMoveStack(Player player, int slot) {
    return ItemStack.EMPTY;
  }

  @Override
  public boolean stillValid(Player player) {
    return true;
  }
}
