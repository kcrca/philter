package net.simplx.mcguidoc;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;

public class DocScreenHandler extends ScreenHandler {

  public DocScreenHandler(int syncId, PlayerInventory playerInventory) {
    this(syncId, playerInventory, 0);
  }

  public DocScreenHandler(int syncId, PlayerInventory playerInventory, int stuff) {
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
