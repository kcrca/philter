package net.simplx.mcgui;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.simplx.philter.Forcer;
import net.simplx.philter.StaticForcer;

public class MinecraftGraphics implements Graphics, Forcer {

  private static final StaticForcer forceScreen = new StaticForcer(HandledScreen.class);
  private static final Field SCREEN_X_F = forceScreen.field("x");
  private static final Field SCREEN_Y_F = forceScreen.field("y");
  private static final Field SCREEN_WIDTH_F = forceScreen.field("backgroundWidth");
  private static final Field SCREEN_HEIGHT_F = forceScreen.field("backgroundHeight");
  private static final Field TEXT_RENDERER_F = forceScreen.field("textRenderer");

  private static final Method ADD_DRAWABLE_CHILD_M = forceScreen.method("addDrawableChild",
      Element.class);

  private final Screen screen;
  private final TextRenderer textRenderer;
  private final int screenX, screenY;
  private final int screenW, screenH;

  public MinecraftGraphics(Screen screen) {
    textRenderer = (TextRenderer) forceGet(screen, TEXT_RENDERER_F);
    screenX = (int) forceGet(screen, SCREEN_X_F);
    screenY = (int) forceGet(screen, SCREEN_Y_F);
    screenW = (int) forceGet(screen, SCREEN_WIDTH_F);
    screenH = (int) forceGet(screen, SCREEN_HEIGHT_F);
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

  @SuppressWarnings("unchecked")
  public <T extends Element & Drawable & Selectable> T addDrawableChild(T element) {
    return (T) forceInvoke(screen, ADD_DRAWABLE_CHILD_M, element);
  }

}
