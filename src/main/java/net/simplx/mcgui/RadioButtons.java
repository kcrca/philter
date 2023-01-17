package net.simplx.mcgui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public class RadioButtons<T> {

  private final List<RadioButtonWidget<T>> buttons;
  private RadioButtonWidget<T> on;

  private BiFunction<RadioButtons<T>, T, Void> updateCallback;

  public RadioButtons() {
    this(null);
  }

  public RadioButtons(BiFunction<RadioButtons<T>, T, Void> updateCallback) {
    this.updateCallback = updateCallback;
    buttons = new ArrayList<>();
  }

  public BiFunction<RadioButtons<T>, T, Void> getUpdateCallback() {
    return updateCallback != null ? updateCallback : null;
  }

  public void setUpdateCallback(
      BiFunction<RadioButtons<T>, T, Void> updateCallback) {
    this.updateCallback = updateCallback;
  }

  public RadioButtonWidget<T> add(RadioButtonWidget<T> button) {
    if (!buttons.contains(button)) {
      buttons.add(button);
      if (button.isChecked() || buttons.size() == 1) {
        setChecked(button);
      }
    }
    button.setButtonsInternal(this, buttons.size() - 1);
    return button;
  }

  public void setChecked(RadioButtonWidget<T> button) {
    for (var b : buttons) {
      b.setCheckedInternal(b == button);
    }
    if (on != button) {
      on = button;
      if (updateCallback != null) {
        updateCallback.apply(this, button.getValue());
      }
    }
  }

  public void remove(RadioButtonWidget<T> button) {
    buttons.remove(button);
    button.setButtonsInternal(null, -1);
    if (on == button) {
      on = null;
    }
  }

  public T getValue() {
    if (on == null) {
      return null;
    }
    return on.getValue();
  }

  public RadioButtonWidget<T> getOn() {
    return on;
  }

  public RadioButtonWidget<T> getButton(int index) {
    return buttons.get(index);
  }

  public RadioButtonWidget<T> findButton(T value) {
    for (RadioButtonWidget<T> button : buttons) {
      if (button.getValue().equals(value)) {
        return button;
      }
    }
    return null;
  }

  public List<RadioButtonWidget<T>> findAllButtons(T value) {
    List<RadioButtonWidget<T>> results = new ArrayList<>();
    for (RadioButtonWidget<T> button : buttons) {
      if (button.getValue().equals(value)) {
        results.add(button);
      }
    }
    return results;
  }
}
