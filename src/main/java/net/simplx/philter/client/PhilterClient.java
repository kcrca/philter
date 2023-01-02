package net.simplx.philter.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.simplx.philter.FilterScreen;
import net.simplx.philter.PhilterMod;

@Environment(EnvType.CLIENT)
public class PhilterClient implements ClientModInitializer {

  @Override
  public void onInitializeClient() {
    HandledScreens.register(PhilterMod.FILTER_SCREEN_HANDLER, FilterScreen::new);
  }
}
