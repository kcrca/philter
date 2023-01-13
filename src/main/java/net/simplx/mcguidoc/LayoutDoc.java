package net.simplx.mcguidoc;

import static net.simplx.mcgui.Horizontal.LEFT;
import static net.simplx.mcgui.Horizontal.RIGHT;
import static net.simplx.mcgui.Vertical.ABOVE;
import static net.simplx.mcgui.Vertical.BELOW;
import static net.simplx.mcgui.Vertical.MID;

import java.util.List;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.LockButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.simplx.mcgui.Layout;
import net.simplx.mcgui.Layout.Placer;
import net.simplx.philter.FilterScreenHandler;

public class LayoutDoc extends HandledScreen<FilterScreenHandler> {

  public LayoutDoc(FilterScreenHandler handler, PlayerInventory inventory, Text title) {
    super(handler, inventory, title);
  }

  @Override
  protected void init() {
    super.init();
    Layout layout = new Layout(this);
    layout.setPrefix("moodle");
    Placer p;
    Text button1Text = layout.text("button1");
    p = layout.placer().withText(button1Text).x(LEFT).y(ABOVE).inButton();
    var button1 = addDrawableChild(
        new ButtonWidget.Builder(button1Text, this::doStuff).dimensions(p.x(), p.y(), p.w(), p.h())
            .build());
    Text button2Text = layout.text("button2");
    String[] colors = new String[]{"red", "green", "blue"};
    p = layout.placer().withTexts(layout.texts(List.of(colors))).x(RIGHT, button1).y(MID, button1)
        .inButton();
    var colorButton = addDrawableChild(
        CyclingButtonWidget.builder(name -> layout.text((String) name))
            .tooltip(name -> layout.tooltip(name + ".tooltip"))
            .build(p.x(), p.y(), p.w(), p.h(), null, (button, name) -> setColor(name)));
    p = layout.placer().lockButton().x(colorButton.getX()).y(BELOW, colorButton);
    addDrawableChild(new LockButtonWidget(p.x(), p.y(), this::toggleLock));
  }

  @Override
  protected void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY) {

  }

  private void doStuff(ButtonWidget button) {

  }

  private void setColor(Object name) {

  }

  private void toggleLock(ButtonWidget button) {

  }

}
