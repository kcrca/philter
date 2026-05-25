package net.simplx.mcgui;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RadioButtonWidgetTest {
  private static final Component EMPTY_TEXT = Component.empty();

  public static final int FIRST_VALUE = 17;

  @Test
  void notOnByDefault() {
    RadioButtonWidget<Integer> button = new RadioButtonWidget<>(FIRST_VALUE, 0, 0, 100, EMPTY_TEXT);
    assertThat(button.isSelected()).isFalse();
  }

  @Test
  void canSetOn() {
    RadioButtonWidget<Integer> button = new RadioButtonWidget<>(FIRST_VALUE, 0, 0, 100, EMPTY_TEXT);
    button.setSelectedInternal(true);
    assertThat(button.isSelected()).isTrue();
  }

  @Test
  void valueIsRemembered() {
    RadioButtonWidget<Integer> button = new RadioButtonWidget<>(FIRST_VALUE, 0, 0, 100, EMPTY_TEXT);
    assertThat(button.getValue()).isEqualTo(FIRST_VALUE);
  }
}
