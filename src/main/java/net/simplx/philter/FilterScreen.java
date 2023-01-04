package net.simplx.philter;

import io.github.cottonmc.cotton.gui.client.CottonInventoryScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public class FilterScreen extends CottonInventoryScreen<FilterGuiDescription> {
  public FilterScreen(FilterGuiDescription gui, PlayerEntity player, Text title) {
    super(gui, player, title);
  }
}
