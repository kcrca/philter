package net.simplx.mcgui;

import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;

interface Graphics {

  int getScreenX();

  int getScreenY();

  int getScreenW();

  int getScreenH();

  int getWidth(String str);

  int getWidth(Component text);

  int getFontHeight();

  void drawText(Component text, float x, float y, int color);

  <T extends GuiEventListener & Renderable & NarratableEntry> T addRenderableWidget(T element);

  int getWindowW();

  int getWindowH();

  void drawTexture(int x, int y, int u, int v, int w, int h);
}
