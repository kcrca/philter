package net.simplx.mcgui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

@SuppressWarnings("unused")
public class RadioButtonWidget<T> extends CheckboxWidget {
  private static final Identifier SELECTED_HIGHLIGHTED_TEXTURE = new Identifier("philter", "widget" +
      "/radiobutton_selected_highlighted");
  private static final Identifier SELECTED_TEXTURE = new Identifier("philter", "widget/radiobutton_selected");
  private static final Identifier HIGHLIGHTED_TEXTURE = new Identifier("philter", "widget/radiobutton_highlighted");
  private static final Identifier TEXTURE = new Identifier("philter", "widget/radiobutton");

  private RadioButtons<T> buttons;
  private final T value;
  private int index;

  public RadioButtonWidget(T value, int x, int y, Text message, TextRenderer textRenderer) {
    this(value, x, y, message, message != null && !message.getString().isBlank(), textRenderer);
  }

  public RadioButtonWidget(T value, int x, int y, Text message, boolean showMessage, TextRenderer textRenderer) {
    super(x, y, message, textRenderer, false, Callback.EMPTY);
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

  public RadioButtons<T> getButtons() {
    return buttons;
  }

  /**
   * Internal call to force this on.
   */
  void setCheckedInternal(boolean on) {
    checked = on;
  }

  /**
   * Internal call to set the buttons.
   */
  void setButtonsInternal(RadioButtons<T> buttons, int index) {
    this.buttons = buttons;
    this.index = index;
  }

  @Override
  public void onPress() {
    if (!isChecked()) {
      super.onPress();
      setChecked(true);
    }
  }

  public void setChecked(boolean on) {
    if (buttons != null) {
      buttons.setChecked(this);
    } else {
      setCheckedInternal(on);
    }
  }

  @Override
  public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
    super.renderWidget(context, mouseX, mouseY, delta);
    MinecraftClient minecraftClient = MinecraftClient.getInstance();
    RenderSystem.enableDepthTest();
    TextRenderer textRenderer = minecraftClient.textRenderer;
    Identifier identifier = this.checked ? (this.isFocused() ? SELECTED_HIGHLIGHTED_TEXTURE : SELECTED_TEXTURE) :
        (this.isFocused() ? HIGHLIGHTED_TEXTURE : TEXTURE);
    int i = CheckboxWidget.getSize(textRenderer);
    context.drawGuiTexture(identifier, this.getX(), this.getY(), i, i);

  }
}
