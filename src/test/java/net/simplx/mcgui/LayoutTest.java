package net.simplx.mcgui;

import static net.simplx.mcgui.Horizontal.CENTER;
import static net.simplx.mcgui.Horizontal.LEFT;
import static net.simplx.mcgui.Horizontal.LEFT_EDGE;
import static net.simplx.mcgui.Horizontal.RIGHT;
import static net.simplx.mcgui.Horizontal.RIGHT_EDGE;
import static net.simplx.mcgui.Vertical.ABOVE;
import static net.simplx.mcgui.Vertical.TOP_EDGE;
import static net.simplx.mcgui.Vertical.BELOW;
import static net.simplx.mcgui.Vertical.BOTOTM_EDGE;
import static net.simplx.mcgui.Vertical.MID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.simplx.mcgui.Layout.Placer;
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
  public static final int SCREEN_X = 11;
  public static final int SCREEN_Y = 26;
  public static final int SCREEN_W = 41;
  public static final int SCREEN_H = 56;
  public static final int GAP_W = 1;
  public static final int GAP_H = 3;
  public static final int BORDER_W = 7;
  public static final int BORDER_H = 13;
  @Mock
  Graphics graphicsMock;
  private Layout layout;

  @BeforeEach
  void setUp() {
    when(graphicsMock.getWidth("n")).thenReturn(EN_W);
    when(graphicsMock.getFontHeight()).thenReturn(FONT_H);
    when(graphicsMock.getScreenX()).thenReturn(SCREEN_X);
    when(graphicsMock.getScreenY()).thenReturn(SCREEN_Y);
    when(graphicsMock.getScreenW()).thenReturn(SCREEN_W);
    when(graphicsMock.getScreenH()).thenReturn(SCREEN_H);
    layout = new Layout(graphicsMock, GAP_W, GAP_H, BORDER_W, BORDER_H);
  }

  @Test
  void init() {
    assertThat(layout.enW).isEqualTo(EN_W);
    assertThat(layout.fontH).isEqualTo(FONT_H);
    assertThat(layout.textH).isGreaterThan(FONT_H);
    assertThat(layout.leadingH).isPositive();
    assertThat(layout.borderW).isEqualTo(BORDER_W);
    assertThat(layout.borderH).isEqualTo(BORDER_H);
    assertThat(layout.gapW).isEqualTo(GAP_W);
    assertThat(layout.gapH).isEqualTo(GAP_H);
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
    when(graphicsMock.getWidth("foo")).thenReturn(17);
    assertThat(layout.textStrW("foo")).isEqualTo(17);
  }

  @Test
  void textW_str() {
    when(graphicsMock.getWidth(Text.translatable("foo"))).thenReturn(17);
    assertThat(layout.textW("foo")).isEqualTo(17);
  }

  @Test
  void maxTextStrW() {
    when(graphicsMock.getWidth("1")).thenReturn(1);
    when(graphicsMock.getWidth("2")).thenReturn(2);
    when(graphicsMock.getWidth("3")).thenReturn(3);
    assertThat(layout.maxTextStrW(List.of("1", "2", "3"))).isEqualTo(3);
  }

  @Test
  void maxTextW() {
    when(graphicsMock.getWidth(Text.translatable("1"))).thenReturn(1);
    when(graphicsMock.getWidth(Text.translatable("2"))).thenReturn(2);
    when(graphicsMock.getWidth(Text.translatable("3"))).thenReturn(3);
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
    when(graphicsMock.getWidth(Text.translatable("foo"))).thenReturn(17);
    assertThat(layout.buttonW("foo")).isGreaterThan(17);
  }

  @Test
  void onOffButtonW() {
    when(graphicsMock.getWidth(Text.translatable("options.on"))).thenReturn(5);
    when(graphicsMock.getWidth(Text.translatable("options.off"))).thenReturn(100);
    when(graphicsMock.getWidth(Text.translatable("foo"))).thenReturn(20);
    assertThat(layout.onOffButtonW(layout.text("foo"))).isGreaterThan(120);
  }

  @Test
  void drawText() {
    layout.drawText(new MatrixStack(), layout.placer().at(17, 29), layout.text("foo"), -1);
    verify(graphicsMock).drawText(Mockito.any(), eq(Text.translatable("foo")), eq(6.0f), eq(3.0f),
        eq(-1));
  }

  @Test
  void addDrawableChild() {
    TextWidget element = new TextWidget(1, 2, 3, 4, layout.text("foo"), null);
    layout.addDrawableChild(element);
    verify(graphicsMock).addDrawableChild(element);
  }

  @Test
  void newPlacer_allUnknown() {
    Placer p = layout.placer();
    assertThat(p.rawX()).isEqualTo(Layout.UNKNOWN);
    assertThat(p.rawY()).isEqualTo(Layout.UNKNOWN);
    assertThat(p.rawW()).isEqualTo(Layout.UNKNOWN);
    assertThat(p.rawH()).isEqualTo(Layout.UNKNOWN);
  }

  @Test
  void bulkSetters() {
    Placer p = layout.placer().size(1, 2).at(3, 4);
    assertThat(p.x()).isEqualTo(3);
    assertThat(p.y()).isEqualTo(4);
    assertThat(p.w()).isEqualTo(1);
    assertThat(p.h()).isEqualTo(2);
  }

  @Test
  void singeSetters() {
    Placer p = layout.placer().w(1).h(2).x(3).y(4);
    assertThat(p.x()).isEqualTo(3);
    assertThat(p.y()).isEqualTo(4);
    assertThat(p.w()).isEqualTo(1);
    assertThat(p.h()).isEqualTo(2);
  }

  @Test
  void relativeWork() {
    Placer p = layout.placer().at(100, 200);
    assertThat(p.relX()).isEqualTo(100 - SCREEN_X);
    assertThat(p.relY()).isEqualTo(200 - SCREEN_Y);
  }

  @Test
  void withText() {
    when(graphicsMock.getWidth(Text.translatable("foo"))).thenReturn(50);
    Placer p = layout.placer().withText("foo");
    assertThat(p.w()).isEqualTo(50);
    assertThat(p.h()).isEqualTo(layout.textH);
  }

  @Test
  void withTexts() {
    when(graphicsMock.getWidth(Text.translatable("foo"))).thenReturn(50);
    when(graphicsMock.getWidth(Text.translatable("bar"))).thenReturn(60);
    when(graphicsMock.getWidth(Text.translatable("baz"))).thenReturn(70);
    Placer p = layout.placer().withTexts("foo", "bar", "baz");
    assertThat(p.w()).isEqualTo(70);
    assertThat(p.h()).isEqualTo(layout.textH);
  }

  @Test
  void relativeLeft() {
    Placer anchorPlacer = layout.placer().w(30).x(10);
    Placer p = layout.placer().w(5).x(LEFT, anchorPlacer);
    assertThat(p.x()).isEqualTo(10 - GAP_W - 5);
  }

  @Test
  void relativeLeftEdge() {
    Placer anchorPlacer = layout.placer().w(30).x(10);
    Placer p = layout.placer().w(5).x(LEFT_EDGE, anchorPlacer);
    assertThat(p.x()).isEqualTo(10);
  }

  @Test
  void relativeCenter() {
    Placer anchorPlacer = layout.placer().w(30).x(10);
    Placer p = layout.placer().w(5).x(CENTER, anchorPlacer);
    assertThat(p.x()).isEqualTo(10 + (30 - 5) / 2);
  }

  @Test
  void relativeRight() {
    Placer anchorPlacer = layout.placer().w(30).x(10);
    Placer p = layout.placer().w(5).x(RIGHT, anchorPlacer);
    assertThat(p.x()).isEqualTo(10 + 30 + GAP_W);
  }

  @Test
  void relativeRightEdge() {
    Placer anchorPlacer = layout.placer().w(30).x(10);
    Placer p = layout.placer().w(5).x(RIGHT_EDGE, anchorPlacer);
    assertThat(p.x()).isEqualTo(10 + 30 - 5);
  }

  @Test
  void relativeLeftScreen() {
    Placer placer = layout.placer().w(5).x(LEFT);
    assertThat(placer.x()).isEqualTo(SCREEN_X + BORDER_W);
  }

  @Test
  void relativeLeftEdgeScreen() {
    Placer placer = layout.placer().w(5).x(LEFT_EDGE);
    assertThat(placer.x()).isEqualTo(SCREEN_X + BORDER_W);
  }

  @Test
  void relativeCenterScreen() {
    Placer placer = layout.placer().w(5).x(CENTER);
    assertThat(placer.x()).isEqualTo(SCREEN_X + (SCREEN_W - 5) / 2);
  }

  @Test
  void relativeRightScreen() {
    Placer placer = layout.placer().w(5).x(RIGHT);
    assertThat(placer.x()).isEqualTo(SCREEN_X + SCREEN_W - BORDER_W - 5);
  }

  @Test
  void relativeRightEdgeScreen() {
    Placer placer = layout.placer().w(5).x(RIGHT_EDGE);
    assertThat(placer.x()).isEqualTo(SCREEN_X + SCREEN_W - BORDER_W - 5);
  }

  @Test
  void relativeAbove() {
    Placer anchorPlacer = layout.placer().h(40).y(20);
    Placer p = layout.placer().h(6).y(ABOVE, anchorPlacer);
    assertThat(p.y()).isEqualTo(20 - GAP_H - 6);
  }

  @Test
  void relativeAboveEdge() {
    Placer anchorPlacer = layout.placer().h(40).y(20);
    Placer p = layout.placer().h(6).y(TOP_EDGE, anchorPlacer);
    assertThat(p.y()).isEqualTo(20);
  }

  @Test
  void relativeMid() {
    Placer anchorPlacer = layout.placer().h(40).y(20);
    Placer p = layout.placer().h(6).y(MID, anchorPlacer);
    assertThat(p.y()).isEqualTo(20 + (40 - 6) / 2);
  }

  @Test
  void relativeBelow() {
    Placer anchorPlacer = layout.placer().h(40).y(20);
    Placer p = layout.placer().h(6).y(BELOW, anchorPlacer);
    assertThat(p.y()).isEqualTo(20 + 40 + GAP_H);
  }

  @Test
  void relativeBelowEdge() {
    Placer anchorPlacer = layout.placer().h(40).y(20);
    Placer p = layout.placer().h(6).y(BOTOTM_EDGE, anchorPlacer);
    assertThat(p.y()).isEqualTo(20 + 40 - 6);
  }

  @Test
  void relativeAboveScreen() {
    Placer placer = layout.placer().h(5).y(ABOVE);
    assertThat(placer.y()).isEqualTo(SCREEN_Y + BORDER_H);
  }

  @Test
  void relativeAboveEdgeScreen() {
    Placer placer = layout.placer().h(5).y(TOP_EDGE);
    assertThat(placer.y()).isEqualTo(SCREEN_Y + BORDER_H);
  }

  @Test
  void relativeMidScreen() {
    Placer placer = layout.placer().h(5).y(MID);
    assertThat(placer.y()).isEqualTo(SCREEN_Y + (SCREEN_H - 5) / 2);
  }

  @Test
  void relativeBelowScreen() {
    Placer placer = layout.placer().h(5).y(BELOW);
    assertThat(placer.y()).isEqualTo(SCREEN_Y + SCREEN_H - BORDER_H - 5);
  }

  @Test
  void relativeBelowScreenEdge() {
    Placer placer = layout.placer().h(5).y(BOTOTM_EDGE);
    assertThat(placer.y()).isEqualTo(SCREEN_Y + SCREEN_H - BORDER_H - 5);
  }

  @Test
  void relativeXWithoutW() {
    for (var dir : List.of(CENTER, RIGHT)) {
      assertThatThrownBy(() -> layout.placer().h(5).x(dir)).isInstanceOf(
          IllegalArgumentException.class);
    }
    layout.placer().h(5).x(LEFT); // no exception

    Placer anchorPlacer = layout.placer(new TextWidget(10, 20, 30, 40, layout.text("foo"), null));
    for (var dir : List.of(CENTER, LEFT)) {
      assertThatThrownBy(() -> layout.placer().h(5).x(dir, anchorPlacer)).isInstanceOf(
          IllegalArgumentException.class);
    }
    layout.placer().h(5).x(RIGHT, anchorPlacer); // no exception
  }

  @Test
  void relativeYWithoutH() {
    for (var dir : List.of(MID, BELOW)) {
      assertThatThrownBy(() -> layout.placer().w(6).y(dir)).isInstanceOf(
          IllegalArgumentException.class);
    }
    layout.placer().h(5).y(ABOVE); // no exception

    Placer anchorPlacer = layout.placer(new TextWidget(10, 20, 30, 40, layout.text("foo"), null));
    for (var dir : List.of(MID, ABOVE)) {
      assertThatThrownBy(() -> layout.placer().w(6).y(dir, anchorPlacer)).isInstanceOf(
          IllegalArgumentException.class);
    }
    layout.placer().h(5).y(BELOW, anchorPlacer); // no exception
  }

  @Test
  void inButton() {
    Placer placer = layout.placer().withText("foo");
    Placer buttoner = placer.clone().inButton();
    // We don't want to assume the adjustments won't change, but they should exist and be positive.
    assertThat(buttoner.w()).isGreaterThan(placer.w());
    assertThat(buttoner.h()).isGreaterThan(placer.h());
  }

  @Test
  void inTextField() {
    Placer placer = layout.placer().withText("foo");
    Placer texter = placer.clone().inTextField();
    // We don't want to assume the adjustments won't change, but they should exist and be positive.
    assertThat(texter.w()).isGreaterThan(placer.w());
    assertThat(texter.h()).isGreaterThan(placer.h());
  }

  @Test
  void fromToHoriz() {
    Placer anchorPlacer = layout.placer(new TextWidget(10, 20, 30, 40, layout.text("foo"), null));
    Placer p = layout.placer().from(LEFT, anchorPlacer).to(RIGHT, anchorPlacer);
    assertThat(p.w()).isEqualTo(30);

    p = layout.placer().from(LEFT).to(RIGHT);
    assertThat(p.w()).isEqualTo(SCREEN_W - 2 * BORDER_W);
  }

  @Test
  void fromToVert() {
    Placer anchorWidget = layout.placer(new TextWidget(10, 20, 30, 40, layout.text("foo"), null));
    Placer p = layout.placer().h(6).from(ABOVE, anchorWidget).to(BELOW, anchorWidget);
    assertThat(p.h()).isEqualTo(46);

    p = layout.placer().from(ABOVE).to(BELOW);
    assertThat(p.h()).isEqualTo(SCREEN_H - 2 * BORDER_H);
  }
}
