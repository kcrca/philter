package net.simplx.mctest;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class StubScreen extends Screen {

  public StubScreen() {
    super(Text.empty());
    init(new StubClient(), 400, 200);
  }
}
