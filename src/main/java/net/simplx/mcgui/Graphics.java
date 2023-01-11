package net.simplx.mcgui;

import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

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

}
