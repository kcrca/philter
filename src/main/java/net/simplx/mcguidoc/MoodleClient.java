package net.simplx.mcguidoc;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

@Environment(EnvType.CLIENT)
public class MoodleClient implements ClientModInitializer {

  @Override
  public void onInitializeClient() {
    HandledScreens.register(MoodleMod.DOC_SCREEN_HANDLER, DocScreen::new);
  }
}
