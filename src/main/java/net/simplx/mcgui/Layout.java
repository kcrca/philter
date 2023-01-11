package net.simplx.mcgui;

import static com.google.common.collect.Streams.stream;

import com.google.common.collect.Lists;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
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

@SuppressWarnings("ALL")
public class Layout implements Forcer {

  public class Placer implements Cloneable {

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

    @Override
    public Placer clone() {
      try {
        return (Placer) super.clone();
      } catch (CloneNotSupportedException e) {
        throw new IllegalStateException(e);
      }
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

    public Placer at(int x, int y) {
      this.x = x;
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

    public Placer size(int w, int h) {
      this.w = w;
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

    public Placer x(Horizontal dir, ClickableWidget to) {
      return x(dir, to.getX(), to.getWidth());
    }

    public Placer x(Horizontal dir, Placer to) {
      return x(dir, to.x(), to.w());
    }

    private Placer x(Horizontal dir, int toX, int toW) {
      x = switch (dir) {
        case LEFT -> toX - gapH - w();
        case CENTER -> toX + (toW - w()) / 2;
        case RIGHT -> toX + toW + gapH;
      };
      return this;
    }

    public Placer y(Vertical dir, ClickableWidget to) {
      return y(dir, to.getY(), to.getHeight());
    }

    public Placer y(Vertical dir, Placer to) {
      return y(dir, to.y(), to.h());
    }

    private Placer y(Vertical dir, int toY, int toH) {
      y = switch (dir) {
        case ABOVE -> toY - gapH - h();
        case MID -> toY + (toH - h()) / 2;
        case BELOW -> toY + toH + gapH;
      };
      return this;
    }

    public Placer x(Horizontal dir) {
      x = switch (dir) {
        case LEFT -> screenX + borderW;
        case CENTER -> screenX + (screenY - w()) / 2;
        case RIGHT -> screenX + screenW - borderW - w();
      };
      return this;
    }

    public Placer y(Vertical dir) {
      y = switch (dir) {
        case ABOVE -> screenY + borderH;
        case MID -> screenY + (screenH - h()) / 2;
        case BELOW -> screenY + screenH - borderH - h();
      };
      return this;
    }

    private void validateDims() {
      validate(w, "w");
      validate(h, "h");
    }


    public Placer inButton() {
      if (w != UNKNOWN) {
        w += 2 * buttonBorderW;
      }
      if (h != UNKNOWN) {
        h += 2 * buttonBorderH;
      }
      return this;
    }

    public Placer inTextField() {
      if (w != UNKNOWN) {
        w += 2;
      }
      if (h == UNKNOWN) {
        h = textH;
      } else {
        h += 2;
      }
      return this;
    }

    private static int coord(Horizontal dir, Placer placer) {
      return switch (dir) {
        case LEFT -> placer.x();
        case CENTER -> placer.x() + placer.w() / 2;
        case RIGHT -> placer.x() + placer.w();
      };
    }

    private static int coord(Horizontal dir, ClickableWidget widget) {
      return switch (dir) {
        case LEFT -> widget.getX();
        case CENTER -> widget.getX() + widget.getWidth();
        case RIGHT -> widget.getX() + widget.getWidth();
      };
    }

    private int coord(Horizontal dir) {
      return switch (dir) {
        case LEFT -> screenX + borderW;
        case CENTER -> screenX + screenW / 2;
        case RIGHT -> screenX + screenW - borderW;
      };
    }

    public record _ToClauseW(Placer thisPlacer, int startX) {

      private Placer extract(int w) {
        thisPlacer.w(w - startX);
        if (thisPlacer.x == UNKNOWN) {
          thisPlacer.x(startX);
        }
        return thisPlacer;
      }

      public Placer to(Horizontal dir, Placer placer) {
        return extract(coord(dir, placer));
      }

      public Placer to(Horizontal dir, ClickableWidget widget) {
        return extract(coord(dir, widget));
      }

      public Placer to(Horizontal dir) {
        return extract(thisPlacer.coord(dir));
      }
    }

    public _ToClauseW from(Horizontal dir, Placer placer) {
      return new _ToClauseW(this, coord(dir, placer));
    }

    public _ToClauseH from(Horizontal dir, ClickableWidget widget) {
      return new _ToClauseH(this, coord(dir, widget));
    }

    public _ToClauseH from(Horizontal dir) {
      return new _ToClauseH(this, coord(dir));
    }

    private static int coord(Vertical dir, Placer placer) {
      return switch (dir) {
        case ABOVE -> placer.y();
        case MID -> placer.y() + placer.h() / 2;
        case BELOW -> placer.y() + placer.h();
      };
    }

    private static int coord(Vertical dir, ClickableWidget widget) {
      return switch (dir) {
        case ABOVE -> widget.getY();
        case MID -> widget.getY() + widget.getHeight();
        case BELOW -> widget.getY() + widget.getHeight();
      };
    }

    private int coord(Vertical dir) {
      return switch (dir) {
        case ABOVE -> screenY + borderH;
        case MID -> screenY + screenH / 2;
        case BELOW -> screenY + screenH - borderH;
      };
    }

    public record _ToClauseH(Placer thisPlacer, int startY) {

      private Placer extract(int coord) {
        thisPlacer.h(coord - startY);
        if (thisPlacer.y == UNKNOWN) {
          thisPlacer.y = coord;
        }
        return thisPlacer;
      }

      public Placer to(Vertical dir, Placer placer) {
        return extract(coord(dir, placer));
      }

      public Placer to(Vertical dir, ClickableWidget widget) {
        return extract(coord(dir, widget));
      }

      public Placer to(Vertical dir) {
        return extract(thisPlacer.coord(dir));
      }
    }

    public _ToClauseH from(Vertical dir, Placer placer) {
      return new _ToClauseH(this, coord(dir, placer));
    }

    public _ToClauseH from(Vertical dir, ClickableWidget widget) {
      return new _ToClauseH(this, coord(dir, widget));
    }

    public _ToClauseH from(Vertical dir) {
      return new _ToClauseH(this, coord(dir));
    }
  }

  public static final int DEFAULT_GAP = 2;
  public static final int DEFAULT_BORDER = 6;

  public static final int UNKNOWN = Integer.MIN_VALUE;

  private static final StaticForcer forceScreen = new StaticForcer(HandledScreen.class);

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
  public final int borderW, borderH;
  public final int gapW, gapH;

  private final Screen screen;
  private final TextRenderer textRenderer;
  private final int screenX, screenY;
  private final int screenW, screenH;
  private final int buttonBorderW, buttonBorderH;

  private String prefix;

  public Layout(Screen screen) {
    this(screen, DEFAULT_BORDER, DEFAULT_BORDER);
  }

  public Layout(Screen screen, int gap, int border) {
    this(screen, gap, gap, border, border);
  }

  public Layout(Screen screen, int gapW, int gapH, int borderW, int borderH) {
    this.screen = screen;
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
