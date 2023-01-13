package net.simplx.mcguidoc;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.simplx.mcgui.Layout;

public class DocScreen extends HandledScreen<DocScreenHandler> {

  private static final Identifier TEXTURE = new Identifier("minecraft",
      "textures/gui/container/dispenser.png");

  private Layout layout;

  public DocScreen(DocScreenHandler handler, PlayerInventory inventory, Text title) {
    super(handler, inventory, title);
  }

  @Override
  protected void init() {
    super.init();
    layout = new Layout(this);
  }

  @Override
  protected void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY) {
    layout.drawBackground(matrices, TEXTURE, delta, mouseX, mouseY);
  }
}
