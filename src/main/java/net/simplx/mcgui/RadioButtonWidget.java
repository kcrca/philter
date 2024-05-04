package net.simplx.mcgui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.text.Text;

@SuppressWarnings("unused")
public class RadioButtonWidget<T> extends CheckboxWidget {

  private RadioButtons<T> buttons;
  private final T value;
  private int index;

  public RadioButtonWidget(T value, int x, int y, Text message, TextRenderer textRenderer) {
    this(value, x, y, message, message != null && !message.getString().isBlank(), textRenderer);
  }

  public RadioButtonWidget(T value, int x, int y, Text message, boolean showMessage, TextRenderer textRenderer) {
    super(x, y, message, textRenderer, false, null);
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
}
