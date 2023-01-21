package net.simplx.mcgui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

class MinecraftGraphics implements Graphics {

  private final HandledScreen<?> screen;
  private final TextRenderer textRenderer;
  private final int windowW, windowH;
  private final int screenX, screenY;
  private final int screenW, screenH;

  public MinecraftGraphics(HandledScreen<?> screen) {
    textRenderer = screen.textRenderer;
    screenX = screen.x;
    screenY = screen.y;
    screenW = screen.backgroundWidth;
    screenH = screen.backgroundHeight;
    windowW = screen.width;
    windowH = screen.height;
    this.screen = screen;
  }

  @Override
  public int getScreenX() {
    return screenX;
  }

  @Override
  public int getScreenY() {
    return screenY;
  }

  @Override
  public int getScreenW() {
    return screenW;
  }

  @Override
  public int getScreenH() {
    return screenH;
  }

  @Override
  public int getWindowW() {
    return windowW;
  }

  @Override
  public int getWindowH() {
    return windowH;
  }

  @Override
  public void drawTexture(MatrixStack matrices, int x, int y, int u, int v, int w, int h) {
    screen.drawTexture(matrices, x, y, u, v, w, h);
  }

  @Override
  public int getWidth(String str) {
    return textRenderer.getWidth(str);
  }

  public int getWidth(Text text) {
    return textRenderer.getWidth(text);
  }

  public int getFontHeight() {
    return textRenderer.fontHeight;
  }

  public void drawText(MatrixStack matrices, Text text, float x, float y, int color) {
    textRenderer.draw(matrices, text, x, y, color);
  }

  public <T extends Element & Drawable & Selectable> T addDrawableChild(T element) {
    return screen.addDrawableChild(element);
  }
}
