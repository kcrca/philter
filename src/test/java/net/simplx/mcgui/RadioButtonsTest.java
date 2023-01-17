package net.simplx.mcgui;

import static org.assertj.core.api.Assertions.assertThat;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

class RadioButtonsTest {

  @SuppressWarnings("SameParameterValue")
  @NotNull
  private static RadioButtons<Integer> createButtonsWithValueOn(int which) {
    RadioButtons<Integer> buttons = new RadioButtons<>();
    for (int i = 0; i < 5; i++) {
      RadioButtonWidget<Integer> button = new RadioButtonWidget<>(FIRST_VALUE + i, 0, 0, 10, 10,
          null);
      if (i == which) {
        button.setChecked(true);
      }
      buttons.add(button);
    }
    return buttons;
  }

  public static final int FIRST_VALUE = 17;

  @Test
  void noButtons_throws() {
    RadioButtons<Integer> buttons = new RadioButtons<>();
    assertThat(buttons.getValue()).isNull();
  }

  @Test
  void onButton_isChecked() {
    RadioButtons<Integer> buttons = new RadioButtons<>();
    RadioButtonWidget<Integer> button = buttons.add(
        new RadioButtonWidget<>(FIRST_VALUE, 0, 0, 10, 10, null));
    assertThat(buttons.getValue()).isEqualTo(FIRST_VALUE);
    assertThat(buttons.getOn()).isSameAs(button);
  }

  @Test
  void buttonsIsSet() {
    RadioButtons<Integer> buttons = new RadioButtons<>();
    RadioButtonWidget<Integer> button = buttons.add(
        new RadioButtonWidget<>(FIRST_VALUE, 0, 0, 10, 10, null));
    assertThat(button.getButtons()).isSameAs(buttons);
  }

  @Test
  void buttonsChanged() {
    RadioButtons<Integer> buttons1 = new RadioButtons<>();
    RadioButtons<Integer> buttons2 = new RadioButtons<>();
    RadioButtonWidget<Integer> button = new RadioButtonWidget<>(FIRST_VALUE, 0, 0, 10, 10, null);
    button.setButtons(buttons1);
    button.setButtons(buttons2);
    assertThat(button.getButtons()).isSameAs(buttons2);
  }

  @Test
  void buttonsChangedIsSet() {
    RadioButtons<Integer> buttons1 = createButtonsWithValueOn(3);
    RadioButtons<Integer> buttons2 = new RadioButtons<>();
    RadioButtonWidget<Integer> button = buttons1.getOn();
    button.setButtons(buttons2);
    assertThat(buttons1.getOn()).isNull();
    assertThat(buttons2.getOn()).isSameAs(button);
  }

  @Test
  void buttonsChangedSetToNull() {
    RadioButtons<Integer> buttons = new RadioButtons<>();
    RadioButtons<Integer> noButtons = null;
    RadioButtonWidget<Integer> button = new RadioButtonWidget<>(FIRST_VALUE, 0, 0, 10, 10, null);
    button.setButtons(buttons);
    button.setButtons(noButtons);
    assertThat(buttons.getOn()).isNull();
    assertThat(button.getButtons()).isNull();
  }

  @Test
  void firstButtonIsChecked() {
    RadioButtons<Integer> buttons = new RadioButtons<>();
    RadioButtonWidget<Integer> first = null;
    for (int i = 0; i < 5; i++) {
      RadioButtonWidget<Integer> button = new RadioButtonWidget<>(FIRST_VALUE + i, 0, 0, 10, 10,
          null);
      if (first == null) {
        first = button;
      }
      buttons.add(button);
    }
    assertThat(buttons.getValue()).isEqualTo(FIRST_VALUE);
    assertThat(buttons.getOn()).isSameAs(first);
  }

  @Test
  void removeButton_noneOn() {
    RadioButtons<Integer> buttons = createButtonsWithValueOn(3);
    buttons.remove(buttons.getOn());
    assertThat(buttons.getOn()).isNull();
  }

  @Test
  void checkedLaterButtonIsSeen() {
    RadioButtons<Integer> buttons = createButtonsWithValueOn(3);
    assertThat(buttons.getValue()).isEqualTo(FIRST_VALUE + 3);
  }

  @Test
  void findButton() {
    RadioButtons<Integer> buttons = createButtonsWithValueOn(3);
    assertThat(buttons.findButton(FIRST_VALUE + 2).getValue()).isEqualTo(FIRST_VALUE + 2);
  }

  @Test
  void changeButton() {
    RadioButtons<Integer> buttons = createButtonsWithValueOn(3);
    buttons.setChecked(buttons.findButton(FIRST_VALUE + 2));
    assertThat(buttons.getValue()).isEqualTo(FIRST_VALUE + 2);
  }

  @Test
  void unsetButton() {
    RadioButtons<Integer> buttons = createButtonsWithValueOn(3);
    buttons.setChecked(null);
    assertThat(buttons.getOn()).isNull();
  }
}