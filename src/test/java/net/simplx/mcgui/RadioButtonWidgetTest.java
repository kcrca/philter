package net.simplx.mcgui;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RadioButtonWidgetTest {

  public static final int FIRST_VALUE = 17;

  @Test
  void notOnByDefault() {
    RadioButtonWidget<Integer> button = new RadioButtonWidget<>(FIRST_VALUE, 0, 0, 10, 10, null);
    assertThat(button.isChecked()).isFalse();
  }

  @Test
  void canSetOn() {
    RadioButtonWidget<Integer> button = new RadioButtonWidget<>(FIRST_VALUE, 0, 0, 10, 10, null);
    button.setCheckedInternal(true);
    assertThat(button.isChecked()).isTrue();
  }

  @Test
  void valueIsRemembered() {
    RadioButtonWidget<Integer> button = new RadioButtonWidget<>(FIRST_VALUE, 0, 0, 10, 10, null);
    assertThat(button.getValue()).isEqualTo(FIRST_VALUE);
  }
}