package net.simplx.mcgui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.NotImplementedException;

class MinecraftGraphics implements Graphics {

  private final AbstractContainerScreen<?> screen;
  private final Font font;
  private final int windowW, windowH;
  private final int screenX, screenY;
  private final int screenW, screenH;

  public MinecraftGraphics(AbstractContainerScreen<?> screen) {
    font = screen.font;
    screenX = screen.leftPos;
    screenY = screen.topPos;
    screenW = screen.imageWidth;
    screenH = screen.imageHeight;
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
  public void drawTexture(int x, int y, int u, int v, int w, int h) {
    throw new NotImplementedException();
  }

  @Override
  public int getWidth(String str) {
    return font.width(str);
  }

  public int getWidth(Component text) {
    return font.width(text);
  }

  public int getFontHeight() {
    return font.lineHeight;
  }

  public void drawText(Component text, float x, float y, int color) {
    throw new NotImplementedException();
  }

  public <T extends GuiEventListener & Renderable & NarratableEntry> T addRenderableWidget(T element) {
    return screen.addRenderableWidget(element);
  }
}
