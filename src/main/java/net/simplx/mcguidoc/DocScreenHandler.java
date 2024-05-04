package net.simplx.mcguidoc;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;

public class DocScreenHandler extends ScreenHandler {

  public DocScreenHandler(int syncId, PlayerInventory playerInventory, MoodleMod.DummyData stuff) {
    this(syncId, playerInventory, null, stuff);
  }

  public DocScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, MoodleMod.DummyData stuff) {
    super(MoodleMod.DOC_SCREEN_HANDLER, syncId);
  }

  @Override
  public ItemStack quickMove(PlayerEntity player, int slot) {
    return null;
  }

  @Override
  public boolean canUse(PlayerEntity player) {
    return true;
  }
}
