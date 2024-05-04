package net.simplx.mcgui;

import static org.assertj.core.api.Assertions.assertThat;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;
import org.junit.jupiter.api.Test;

class RadioButtonWidgetTest {
  private static final Text EMPTY_TEXT = Text.empty();
  private static final TextRenderer DUMMY_RENDERER = new DummyRenderer();

  public static final int FIRST_VALUE = 17;

  @Test
  void notOnByDefault() {
    RadioButtonWidget<Integer> button = new RadioButtonWidget<>(FIRST_VALUE, 0, 0,EMPTY_TEXT, DUMMY_RENDERER);
    assertThat(button.isChecked()).isFalse();
  }

  @Test
  void canSetOn() {
    RadioButtonWidget<Integer> button = new RadioButtonWidget<>(FIRST_VALUE, 0, 0, EMPTY_TEXT, DUMMY_RENDERER);
    button.setCheckedInternal(true);
    assertThat(button.isChecked()).isTrue();
  }

  @Test
  void valueIsRemembered() {
    RadioButtonWidget<Integer> button = new RadioButtonWidget<>(FIRST_VALUE, 0, 0, EMPTY_TEXT, DUMMY_RENDERER);
    assertThat(button.getValue()).isEqualTo(FIRST_VALUE);
  }
}