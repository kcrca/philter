package net.simplx.mcgui;

import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

/**
 * This interface exists for testing. The minecraft code uses lots of fields, and fields aren't mockable. So to test
 * something that uses Screen (like all of McGUI), we need to wrap that stuff inside an interface so we can mock that.
 * This code is used internally.
 */
interface Graphics {

  int getScreenX();

  int getScreenY();

  int getScreenW();

  int getScreenH();

  int getWidth(String str);

  int getWidth(Text text);

  int getFontHeight();

  void drawText(MatrixStack matrices, Text text, float x, float y, int color);

  <T extends Element & Drawable & Selectable> T addDrawableChild(T element);

  int getWindowW();

  int getWindowH();

  void drawTexture(MatrixStack matrices, int x, int y, int u, int v, int w, int h);
}
