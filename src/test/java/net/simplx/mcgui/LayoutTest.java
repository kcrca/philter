package net.simplx.mcgui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LayoutTest {

  public static final int EN_W = 4;
  public static final int FONT_H = 10;
  @Mock
  Graphics grahpicsMock;
  private Layout layout;

  @BeforeEach
  void setUp() {
    when(grahpicsMock.getWidth("n")).thenReturn(EN_W);
    when(grahpicsMock.getFontHeight()).thenReturn(FONT_H);
    when(grahpicsMock.getScreenX()).thenReturn(10);
    when(grahpicsMock.getScreenY()).thenReturn(20);
    when(grahpicsMock.getScreenW()).thenReturn(30);
    when(grahpicsMock.getScreenH()).thenReturn(40);
    layout = new Layout(grahpicsMock);
  }

  @Test
  void init() {
    assertThat(layout.enW).isEqualTo(EN_W);
    assertThat(layout.fontH).isEqualTo(FONT_H);
    assertThat(layout.textH).isGreaterThan(FONT_H);
    assertThat(layout.leadingH).isPositive();
    assertThat(layout.borderW).isEqualTo(Layout.DEFAULT_BORDER);
    assertThat(layout.borderH).isEqualTo(Layout.DEFAULT_BORDER);
    assertThat(layout.gapW).isEqualTo(Layout.DEFAULT_GAP);
    assertThat(layout.gapH).isEqualTo(Layout.DEFAULT_GAP);
  }

  @Test
  void setPrefix_noPrefix() {
    Text text = layout.text("foo");
    assertThat(text.getString()).isEqualTo("foo");
  }

  @Test
  void setPrefix_prefixUsed() {
    layout.setPrefix("testing.");
    Text text = layout.text("foo");
    assertThat(text.getString()).isEqualTo("testing.foo");
  }

  @Test
  void setPrefix_dotAdded() {
    layout.setPrefix("testing");
    Text text = layout.text("foo");
    assertEquals("testing.foo", text.getString());
  }

  @Test
  void textStrW_str() {
    when(grahpicsMock.getWidth("foo")).thenReturn(17);
    assertThat(layout.textStrW("foo")).isEqualTo(17);
  }

  @Test
  void textW_str() {
    when(grahpicsMock.getWidth(Text.translatable("foo"))).thenReturn(17);
    assertThat(layout.textW("foo")).isEqualTo(17);
  }

  @Test
  void maxTextStrW() {
    when(grahpicsMock.getWidth("1")).thenReturn(1);
    when(grahpicsMock.getWidth("2")).thenReturn(2);
    when(grahpicsMock.getWidth("3")).thenReturn(3);
    assertThat(layout.maxTextStrW(List.of("1", "2", "3"))).isEqualTo(3);
  }

  @Test
  void maxTextW() {
    when(grahpicsMock.getWidth(Text.translatable("1"))).thenReturn(1);
    when(grahpicsMock.getWidth(Text.translatable("2"))).thenReturn(2);
    when(grahpicsMock.getWidth(Text.translatable("3"))).thenReturn(3);
    assertThat(layout.maxTextW(layout.texts(List.of("1", "2", "3")))).isEqualTo(3);
  }

  @Test
  void text() {
    assertThat(layout.text("foo.bar").getString()).isEqualTo("foo.bar");
  }

  @Test
  void literal() {
    assertThat(layout.literal("foo.bar").getString()).isEqualTo("foo.bar");
  }

  @Test
  void tooltip() {
    // I can't examine the guts of this in any way I know.
    assertThat(layout.tooltip("foo.bar")).isNotNull();
  }

  @Test
  void buttonW() {
    when(grahpicsMock.getWidth(Text.translatable("foo"))).thenReturn(17);
    assertThat(layout.buttonW("foo")).isGreaterThan(17);
  }

  @Test
  void onOffButtonW() {
    when(grahpicsMock.getWidth(Text.translatable("options.on"))).thenReturn(5);
    when(grahpicsMock.getWidth(Text.translatable("options.off"))).thenReturn(100);
    when(grahpicsMock.getWidth(Text.translatable("foo"))).thenReturn(20);
    assertThat(layout.onOffButtonW(layout.text("foo"))).isGreaterThan(120);
  }

  @Test
  void drawText() {
    layout.drawText(new MatrixStack(), layout.placer().at(17, 29), layout.text("foo"), -1);
    verify(grahpicsMock).drawText(Mockito.any(), eq(Text.translatable("foo")), eq(7.0f), eq(9.0f),
        eq(-1));
  }

  @Test
  void addDrawableChild() {
    var element = new TextWidget(1, 2, 3, 4, layout.text("foo"), null);
    layout.addDrawableChild(element);
    verify(grahpicsMock).addDrawableChild(element);
  }
}