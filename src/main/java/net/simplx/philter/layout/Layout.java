package net.simplx.philter.layout;

import static com.google.common.collect.Streams.stream;

import com.google.common.collect.Lists;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.simplx.philter.Forcer;
import net.simplx.philter.StaticForcer;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("ALL")
public class Layout implements Forcer {

  public class Placer {

    private int x, y;
    private int w, h;

    Placer(int x, int y, int w, int h) {
      this.x = x;
      this.y = y;
      this.w = w;
      this.h = x;
    }

    Placer() {
      this(UNKNOWN, UNKNOWN, UNKNOWN, UNKNOWN);
    }

    public int x() {
      validate(x, "x");
      return x;
    }

    public int y() {
      validate(y, "y");
      return y;
    }

    /**
     * Coord is screen relative.
     */
    public int relX() {
      validate(x, "x");
      return x - screenX;
    }

    /**
     * Coord is screen relative.
     */
    public int relY() {
      validate(y, "y");
      return y - screenY;
    }

    public int w() {
      validate(w, "w");
      return w;
    }

    public int h() {
      validate(h, "h");
      return h;
    }

    public Placer x(int x) {
      this.x = x;
      return this;
    }

    public Placer y(int y) {
      this.y = y;
      return this;
    }

    /**
     * Coord is screen relative.
     */
    public Placer relX(int x) {
      this.x = x + screenX;
      return this;
    }

    /**
     * Coord is screen relative.
     */
    public Placer relY(int y) {
      this.y = y + screenY;
      return this;
    }

    public Placer w(int w) {
      this.w = w;
      return this;
    }

    public Placer h(int h) {
      this.h = h;
      return this;
    }

    public Placer withText(String keyName) {
      return withText(text(keyName));
    }

    public Placer withText(Text text) {
      return w(textW(text)).h(textH);
    }

    public Placer withTexts(String keyName, String... keyNames) {
      return withTexts(texts(keyName, keyNames));
    }

    public Placer withTexts(Text text, Text... others) {
      return withTexts(Lists.asList(text, others));
    }

    public Placer withTexts(Iterable<Text> texts) {
      return w(maxTextW(texts)).h(textH);
    }

    private void validate(int val, String name) {
      if (val == UNKNOWN) {
        throw new IllegalArgumentException(name + ": Not yet set");
      }
    }

    public Placer align(Horizontal dir, ClickableWidget to) {
      int toX = (int) forceGet(to, WIDGET_X_F);
      int toW = (int) forceGet(to, WIDGET_WIDTH_F);
      return align(dir, toX, toW);
    }

    public Placer align(Horizontal dir, Placer to) {
      return align(dir, to.x(), to.w());
    }

    private Placer align(Horizontal dir, int toX, int toW) {
      x = switch (dir) {
        case LEFT -> toX - gapH - w();
        case CENTER -> toX + (toW - w()) / 2;
        case RIGHT -> toX + toW + gapH;
      };
      return this;
    }

    public Placer align(Vertical dir, ClickableWidget to) {
      int toY = (int) forceGet(to, WIDGET_Y_F);
      int toH = (int) forceGet(to, WIDGET_HEIGHT_F);
      return align(dir, toY, toH);
    }

    public Placer align(Vertical dir, Placer to) {
      return align(dir, to.y(), to.h());
    }

    private Placer align(Vertical dir, int toY, int toH) {
      y = switch (dir) {
        case ABOVE -> toY - gapH - h();
        case MID -> toY + (toH - h()) / 2;
        case BELOW -> toY + toH + gapH;
      };
      return this;
    }

    public Placer align(Horizontal dir) {
      x = switch (dir) {
        case LEFT -> screenX + borderW;
        case CENTER -> screenX + (screenY - w()) / 2;
        case RIGHT -> screenX + screenW - borderW - w();
      };
      return this;
    }

    public Placer align(Vertical dir) {
      y = switch (dir) {
        case ABOVE -> screenY + borderH;
        case MID -> screenY + (screenH - h()) / 2;
        case BELOW -> screenY + screenH - borderH - h();
      };
      return this;
    }


    public Placer inButton() {
      w += 2 * buttonBorderW;
      h += 2 * buttonBorderH;
      return this;
    }
  }

  public static final int DEFAULT_GAP = 2;
  public static final int DEFAULT_BORDER = 6;

  public static final int UNKNOWN = Integer.MIN_VALUE;

  private static final StaticForcer forceScreen = new StaticForcer(HandledScreen.class);
  private static final StaticForcer forceElement = new StaticForcer(ClickableWidget.class);

  private static final Field WIDGET_X_F = forceElement.field("x");
  private static final Field WIDGET_Y_F = forceElement.field("y");
  private static final Field WIDGET_WIDTH_F = forceElement.field("width");
  private static final Field WIDGET_HEIGHT_F = forceElement.field("height");

  private static final Field SCREEN_X_F = forceScreen.field("x");
  private static final Field SCREEN_Y_F = forceScreen.field("y");
  private static final Field SCREEN_WIDTH_F = forceScreen.field("backgroundWidth");
  private static final Field SCREEN_HEIGHT_F = forceScreen.field("backgroundHeight");
  private static final Field TEXT_RENDERER_F = forceScreen.field("textRenderer");

  private static final Method ADD_DRAWABLE_CHILD_M = forceScreen.method("addDrawableChild",
      Element.class);
  private static final Method REMOVE_M = forceScreen.method("remove", Element.class);

  public final int enW;
  public final int fontH;
  public final int textH;
  public final int leadingH;

  private final Screen screen;
  private final int screenWidth, screenHeight;
  private final int gapW, gapH;
  private final int borderW, borderH;
  private final TextRenderer textRenderer;
  private final int screenX, screenY;
  private final int screenW, screenH;
  private final int buttonBorderW, buttonBorderH;

  private String prefix;

  public Layout(Screen screen, int w, int h) {
    this(screen, w, h, DEFAULT_GAP, DEFAULT_GAP, DEFAULT_BORDER, DEFAULT_BORDER);
  }

  public Layout(Screen screen, int w, int h, int gap, int border) {
    this(screen, w, h, gap, gap, border, border);
  }

  public Layout(Screen screen, int width, int height, int gapW, int gapH, int borderW,
      int borderH) {
    this.screen = screen;
    screenWidth = width;
    screenHeight = height;
    this.gapW = gapW;
    this.gapH = gapH;
    this.borderW = borderW;
    this.borderH = borderH;
    this.textRenderer = (TextRenderer) forceGet(screen, TEXT_RENDERER_F);
    enW = textRenderer.getWidth("n");
    fontH = textRenderer.fontHeight;
    leadingH = Math.round(fontH * 0.2f);
    textH = fontH + leadingH;
    screenX = (int) forceGet(screen, SCREEN_X_F);
    screenY = (int) forceGet(screen, SCREEN_Y_F);
    screenW = (int) forceGet(screen, SCREEN_WIDTH_F);
    screenH = (int) forceGet(screen, SCREEN_HEIGHT_F);
    buttonBorderW = 2 * enW;
    buttonBorderH = Math.round(fontH * 0.3f);
    prefix = "";
  }

  public Placer placer() {
    return new Placer();
  }

  public void setPrefix(String prefix) {
    if (prefix.charAt(prefix.length() - 1) != '.') {
      prefix += ".";
    }
    this.prefix = prefix;
  }

  public Tooltip tooltip(String keyName) {
    return Tooltip.of(text(keyName));
  }

  public MutableText text(String keyName) {
    if (!prefix.isEmpty()) {
      keyName = prefix + keyName;
    }
    return Text.translatable(keyName);
  }

  public MutableText literal(String str) {
    return Text.literal(str);
  }

  public int textW(String keyName) {
    return textW(text(keyName));
  }

  public int textW(Text text) {
    return textRenderer.getWidth(text);
  }

  public Collection<Text> texts(Iterable<String> strs) {
    return stream(strs).map(str -> (Text) text(str)).toList();
  }

  public Collection<Text> texts(String keyName, String... others) {
    return texts(Lists.asList(keyName, others));
  }

  public int maxTextW(Iterable<Text> texts) {
    return stream(texts).map(this::textW).max(Integer::compare).orElse(0);
  }

  public int maxTextW(Text first, Text... others) {
    return maxTextW(Lists.asList(first, others));
  }

  public int buttonW(String text) {
    return buttonW(text(text));
  }

  public int buttonW(Text text) {
    return textW(text) + 2 * enW;
  }

  public int buttonW(Iterable<Text> texts) {
    int maxW = 0;
    for (Text t : texts) {
      maxW = Math.max(buttonW(t), maxW);
    }
    return maxW;
  }

  public int onOffButtonW(Text text) {
    int onOffW = buttonW(
        List.of(Text.translatable("options.on"), Text.translatable("options.off")));
    return textW(text) + textW(Text.literal(": ")) + onOffW;
  }

  public Placer getData(Class<? extends Element> widgetClass, int w, int h, To to, Element other) {
    return getData(w, h, to, (int) forceGet(other, WIDGET_X_F), (int) forceGet(other, WIDGET_Y_F),
        (int) forceGet(other, WIDGET_WIDTH_F), (int) forceGet(other, WIDGET_HEIGHT_F));
  }

  public Placer getData(Class<? extends Element> widgetClass, int w, int h, To to, Screen screen) {
    return getData(w, h, to, (int) forceGet(screen, SCREEN_X_F), (int) forceGet(screen, SCREEN_Y_F),
        (int) forceGet(screen, SCREEN_WIDTH_F), (int) forceGet(screen, SCREEN_HEIGHT_F));
  }

  @NotNull
  public Layout.Placer getButtonData(Placer placer, Text text) {
    int x = fillIn(placer.x, () -> 0);
    int y = fillIn(placer.y, () -> 0);
    int w = fillIn(placer.w, () -> textW(text));
    int h = fillIn(placer.h, () -> textH);
    return new Placer(x, y, w + 2 * enW, h + 2 * 3);
  }

  @NotNull
  public Layout.Placer getTextData(Placer placer, Text text) {
    int x = fillIn(placer.x, () -> 0);
    int y = fillIn(placer.y, () -> 0);
    int contentW = fillIn(placer.w, () -> textW(text));
    int contentH = fillIn(placer.h, () -> textH);
    return new Placer(x, y, contentW, contentH);
  }

  private int fillIn(int value, Supplier<Integer> def) {
    return value == UNKNOWN ? def.get() : value;
  }

  @NotNull
  private Layout.Placer getData(int w, int h, To to, int ox, int oy, int ow, int oh) {
    switch (to) {
      case LEFT:
        return new Placer(ox - w - gapW, oy, w, h);
      case RIGHT:
        return new Placer(ox + ow + gapW, oy, w, h);
      case UP:
        return new Placer(ox, oy - h - gapH, w, h);
      case DOWN:
        return new Placer(ox, oy + oh + gapH, w, h);
      default:
        throw new IllegalArgumentException(to + ": Unknown value");
    }
  }

  public void drawText(MatrixStack matrices, Placer placer, Text text, int color) {
    textRenderer.draw(matrices, text, (float) placer.relX(), (float) placer.relY(), color);
  }

  @SuppressWarnings("unchecked")
  protected <T extends Element & Drawable & Selectable> T addDrawableChild(T element) {
    return (T) forceInvoke(screen, ADD_DRAWABLE_CHILD_M, element);
  }

  protected void remove(Element child) {
    forceInvoke(screen, REMOVE_M, child);
  }
}
