package net.simplx.mcgui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

@SuppressWarnings("unused")
public class RadioButtonWidget<T> extends AbstractWidget.WithInactiveMessage {
  private static final Identifier SELECTED_HIGHLIGHTED_TEXTURE = Identifier.fromNamespaceAndPath("philter",
      "widget/radiobutton_selected_highlighted");
  private static final Identifier SELECTED_TEXTURE = Identifier.fromNamespaceAndPath("philter", "widget/radiobutton_selected");
  private static final Identifier HIGHLIGHTED_TEXTURE = Identifier.fromNamespaceAndPath("philter", "widget/radiobutton_highlighted");
  private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("philter", "widget/radiobutton");

  private RadioButtons<T> buttons;
  private final T value;
  private int index;
  private boolean selected;

  public RadioButtonWidget(T value, int x, int y, int maxWidth, Component message) {
    super(x, y, maxWidth, 20, message);
    this.value = value;
    index = -1;
  }

  public T getValue() {
    return value;
  }

  public int getIndex() {
    return index;
  }

  public RadioButtons<T> buttons() {
    return buttons;
  }

  public RadioButtons<T> getButtons() {
    return buttons;
  }

  public boolean isSelected() {
    return selected;
  }

  public void setButtons(RadioButtons<T> buttons) {
    if (buttons == this.buttons) {
      return;
    }
    if (this.buttons != null) {
      this.buttons.remove(this);
    }
    this.buttons = buttons;
    if (buttons != null) {
      buttons.add(this);
    }
  }

  void setSelectedInternal(boolean on) {
    selected = on;
  }

  void setButtonsInternal(RadioButtons<T> buttons, int index) {
    this.buttons = buttons;
    this.index = index;
  }

  @Override
  public void onClick(MouseButtonEvent event, boolean bl) {
    if (!selected) {
      if (buttons != null) {
        buttons.setChecked(this);
      } else {
        selected = true;
      }
    }
  }

  public void setChecked(boolean on) {
    if (buttons != null) {
      buttons.setChecked(this);
    } else {
      selected = on;
    }
  }

  @Override
  protected void extractWidgetRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float delta) {
    Font font = Minecraft.getInstance().font;
    Identifier id = selected
        ? (isFocused() ? SELECTED_HIGHLIGHTED_TEXTURE : SELECTED_TEXTURE)
        : (isFocused() ? HIGHLIGHTED_TEXTURE : TEXTURE);
    int size = Checkbox.getBoxSize(font);
    extractor.blitSprite(RenderPipelines.GUI_TEXTURED, id, getX(), getY(), size, size);
  }

  @Override
  protected void updateWidgetNarration(NarrationElementOutput output) {
    defaultButtonNarrationText(output);
  }
}
