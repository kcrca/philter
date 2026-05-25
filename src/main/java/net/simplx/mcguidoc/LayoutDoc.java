package net.simplx.mcguidoc;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.LockIconButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.simplx.mcgui.Layout;
import net.simplx.mcgui.Layout.Placer;
import net.simplx.philter.FilterScreenHandler;

import java.util.List;

import static net.simplx.mcgui.Horizontal.LEFT;
import static net.simplx.mcgui.Horizontal.RIGHT;
import static net.simplx.mcgui.Vertical.*;

public class LayoutDoc extends AbstractContainerScreen<FilterScreenHandler> {

  public LayoutDoc(FilterScreenHandler handler, Inventory inventory, Component title) {
    super(handler, inventory, title);
  }

  @Override
  protected void init() {
    super.init();
    Layout layout = new Layout(this);
    layout.setPrefix("moodle");
    Placer p;
    Component button1Text = layout.text("button1");
    Placer button1Placer = p = layout.placer().withText(button1Text).x(LEFT).y(ABOVE).inButton();
    var button1 = addRenderableWidget(
        Button.builder(button1Text, this::doStuff).bounds(p.x(), p.y(), p.w(), p.h()).build());
    String[] colors = new String[]{"red", "green", "blue"};
    Placer colorButtonPlacer = p = layout.placer().withTexts(layout.texts(List.of(colors)))
        .x(RIGHT, button1Placer).y(MID, button1Placer).inButton();
    var colorButton = addRenderableWidget(
        CycleButton.builder(name -> layout.text((String) name), colors[0])
            .withValues(colors)
            .withTooltip(name -> layout.tooltip(name + ".tooltip"))
            .create(p.x(), p.y(), p.w(), p.h(), Component.empty(), (button, name) -> setColor(name)));
    p = layout.placer().lockButton().x(colorButton.getX()).y(BELOW, colorButtonPlacer);
    addRenderableWidget(new LockIconButton(p.x(), p.y(), this::toggleLock));
  }

  @Override
  public void extractContents(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float delta) {
  }

  @Override
  protected void extractLabels(GuiGraphicsExtractor extractor, int mouseX, int mouseY) {
  }

  private void doStuff(Button button) {
  }

  private void setColor(Object name) {
  }

  private void toggleLock(Button button) {
  }
}
