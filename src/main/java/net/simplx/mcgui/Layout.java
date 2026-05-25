package net.simplx.mcgui;

import com.google.common.collect.Lists;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.LockIconButton;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static com.google.common.collect.Streams.stream;

/**
 * {@code Layout} provides tools for laying out Minecraft GUIs using the underlying widgets (or other ones if you
 * prefer). This is typically used in a mod's {@code Screen} class to put up dialog boxes for custom blocks.
 */
@SuppressWarnings({"UnusedReturnValue", "unused"})
public class Layout {
  @SuppressWarnings("unused")
  public class Placer implements Cloneable {

    private int x, y;
    private int w, h;

    Placer() {
      x = y = w = h = UNKNOWN;
    }

    @Override
    public Placer clone() {
      try {
        return (Placer) super.clone();
      } catch (CloneNotSupportedException e) {
        throw new IllegalStateException(e);
      }
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Placer placer = (Placer) o;
      return x == placer.x && y == placer.y && w == placer.w && h == placer.h;
    }

    @Override
    public int hashCode() {
      return Objects.hash(x, y, w, h);
    }
    // Raw methods for testing.

    int rawX() {
      return x;
    }

    int rawY() {
      return y;
    }

    int rawW() {
      return w;
    }

    int rawH() {
      return h;
    }

    boolean hasX() {
      return x != UNKNOWN;
    }

    boolean hasY() {
      return y != UNKNOWN;
    }

    boolean hasW() {
      return w != UNKNOWN;
    }

    boolean hasH() {
      return h != UNKNOWN;
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

    public int endX() {
      return x() + w();
    }

    public int endY() {
      return y() + h();
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

    public Placer withText(Component text) {
      return w(textW(text)).h(textH);
    }

    public Placer withTexts(String keyName, String... keyNames) {
      return withTexts(texts(keyName, keyNames));
    }

    public Placer withTexts(Component text, Component... others) {
      return withTexts(Lists.asList(text, others));
    }

    public Placer withTexts(Iterable<Component> texts) {
      return w(maxTextW(texts)).h(textH);
    }

    private void validate(int val, String name) {
      if (val == UNKNOWN) {
        throw new IllegalArgumentException(name + ": Not yet set");
      }
    }

    public Placer x(Horizontal dir, Placer to) {
      x = switch (dir) {
        case LEFT -> to.x() - gapW - w();
        case CENTER -> to.x() + (to.w() - w()) / 2;
        case RIGHT -> to.x() + to.w() + gapW;
        case LEFT_EDGE -> to.x();
        case RIGHT_EDGE -> to.x() + to.w() - w();
      };
      return this;
    }

    public Placer y(Vertical dir, Placer to) {
      y = switch (dir) {
        case ABOVE -> to.y() - gapH - h();
        case TOP_EDGE -> to.y();
        case TOP -> to.y();
        case MID -> to.y() + (to.h() - h()) / 2;
        case BOTTOM -> to.y() + to.h();
        case BOTTOM_EDGE -> to.y() + to.h() - h();
        case BELOW -> to.y() + to.h() + gapH;
      };
      return this;
    }

    public Placer x(Horizontal dir) {
      x = switch (dir) {
        case LEFT, LEFT_EDGE -> screenX + borderW;
        case CENTER -> screenX + (screenW - w()) / 2;
        case RIGHT, RIGHT_EDGE -> screenX + screenW - borderW - w();
      };
      return this;
    }

    public Placer y(Vertical dir) {
      y = switch (dir) {
        case ABOVE, TOP, TOP_EDGE -> screenY + borderH;
        case MID -> screenY + (screenH - h()) / 2;
        case BELOW, BOTTOM, BOTTOM_EDGE -> screenY + screenH - borderH - h();
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
      } else {
        h = Button.DEFAULT_HEIGHT;
      }
      return this;
    }

    public Placer inTextField() {
      if (w != UNKNOWN) {
        w += 2;
      }
      if (h == UNKNOWN) {
        h = 20;
      } else {
        h += 2;
      }
      return this;
    }

    public Placer inLabel() {
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

    public Placer lockButton() {
      ensureLockButtonData();
      w = lockButtonW;
      h = lockButtonH;
      return this;
    }

    public Placer inCheckbox() {
      // It doesn't say this anywhere, but it's 20x20.
      w = h = 20;
      return this;
    }

    public <T extends AbstractWidget> T place(T widget) {
      if (hasX()) {
        widget.setX(x());
      }
      if (hasY()) {
        widget.setY(y());
      }
      if (hasW()) {
        widget.setWidth(w());
      }
      if (hasH()) {
        widget.setHeight(h());
      }
      return widget;
    }

    public class _ToClauseHoriz {

      private final int startX;

      public _ToClauseHoriz(Placer thisPlacer, int startX) {
        this.startX = startX;
      }

      private Placer extract(int x) {
        w(Math.abs(x - startX));
        if (Placer.this.x == UNKNOWN) {
          x(Math.min(x, startX));
        }
        return Placer.this;
      }

      public Placer to(Horizontal dir, Placer placer) {
        return extract(coord(dir, placer));
      }

      public Placer to(Horizontal dir) {
        return extract(coord(dir));
      }

      public Placer to(int x) {
        return extract(x);
      }
    }

    private int coord(Horizontal dir, Placer placer) {
      return switch (dir) {
        case LEFT, LEFT_EDGE -> placer.x();
        case CENTER -> placer.x() + placer.w() / 2;
        case RIGHT -> placer.x() + placer.w();
        case RIGHT_EDGE -> placer.x() + placer.w() - w();
      };
    }

    private int coord(Horizontal dir) {
      return switch (dir) {
        case LEFT, LEFT_EDGE -> screenX + borderW;
        case CENTER -> screenX + screenW / 2;
        case RIGHT, RIGHT_EDGE -> screenX + screenW - borderW;
      };
    }

    public _ToClauseHoriz from(Horizontal dir, Placer placer) {
      return new _ToClauseHoriz(this, coord(dir, placer));
    }

    public _ToClauseHoriz from(Horizontal dir) {
      return new _ToClauseHoriz(this, coord(dir));
    }

    public class _ToClauseVert {

      private final int startY;

      _ToClauseVert(int startY) {
        this.startY = startY;
      }

      private Placer extract(int y) {
        h(Math.abs(y - startY));
        if (Placer.this.y == UNKNOWN) {
          y(Math.min(y, startY));
        }
        return Placer.this;
      }

      public Placer to(Vertical dir, Placer placer) {
        return extract(coord(dir, placer));
      }

      public Placer to(Vertical dir) {
        return extract(coord(dir));
      }

      public Placer to(int y) {
        return extract(y);
      }
    }

    private int coord(Vertical dir, Placer placer) {
      return switch (dir) {
        case ABOVE -> placer.y() - gapH;
        case TOP -> placer.y();
        case TOP_EDGE -> placer.y() + h();
        case MID -> placer.y() + placer.h() / 2;
        case BOTTOM -> placer.y() + placer.h();
        case BELOW -> placer.y() + placer.h() + gapH;
        case BOTTOM_EDGE -> placer.y() + placer.h() - w();
      };
    }

    private int coord(Vertical dir) {
      return switch (dir) {
        case ABOVE, TOP, TOP_EDGE -> screenY + borderH;
        case MID -> screenY + screenH / 2;
        case BELOW, BOTTOM -> screenY + screenH - borderH;
        case BOTTOM_EDGE -> screenY + screenH - borderH - h();
      };
    }

    public _ToClauseVert from(Vertical dir, Placer placer) {
      return new _ToClauseVert(coord(dir, placer));
    }

    public _ToClauseVert from(Vertical dir) {
      return new _ToClauseVert(coord(dir));
    }
  }

  private void ensureLockButtonData() {
    if (lockButtonW == UNKNOWN) {
      LockIconButton lb = new LockIconButton(0, 0, button -> {});
      lockButtonW = lb.getWidth();
      lockButtonH = lb.getHeight();
    }
  }

  public static final int DEFAULT_GAP = 2;
  public static final int DEFAULT_BORDER = 6;

  public static final int UNKNOWN = Integer.MIN_VALUE;

  public final int enW;
  public final int fontH;
  public final int textH;
  public final int leadingH;
  public final int borderW, borderH;
  public final int gapW, gapH;

  public final int slotW, slotH;

  private final Graphics graphics;
  private final int screenX, screenY;
  private final int screenW, screenH;
  private final int buttonBorderW, buttonBorderH;

  private int lockButtonW = UNKNOWN, lockButtonH = UNKNOWN;

  private String prefix;

  public Layout(AbstractContainerScreen<?> screen) {
    this(screen, DEFAULT_BORDER, DEFAULT_BORDER);
  }

  public Layout(AbstractContainerScreen<?> screen, int gap, int border) {
    this(screen, gap, gap, border, border);
  }

  public Layout(AbstractContainerScreen<?> screen, int gapW, int gapH, int borderW, int borderH) {
    this(new MinecraftGraphics(screen), gapW, gapH, borderW, borderH);
  }

  Layout(Graphics graphics) {
    this(graphics, DEFAULT_GAP, DEFAULT_GAP, DEFAULT_BORDER, DEFAULT_BORDER);
  }

  Layout(Graphics graphics, int gapW, int gapH, int borderW, int borderH) {
    this.graphics = graphics;
    this.gapW = gapW;
    this.gapH = gapH;
    this.borderW = borderW;
    this.borderH = borderH;
    enW = graphics.getWidth("n");
    fontH = graphics.getFontHeight();
    leadingH = Math.round(fontH * 0.2f);
    textH = fontH + leadingH;
    screenX = graphics.getScreenX();
    screenY = graphics.getScreenY();
    screenW = graphics.getScreenW();
    screenH = graphics.getScreenH();
    buttonBorderW = enW;
    buttonBorderH = Math.round(fontH * 0.3f);

    slotW = 18;
    slotH = 18;

    prefix = "";
  }

  public Placer placer() {
    return new Placer();
  }

  public Placer placer(AbstractWidget widget) {
    var placer = new Placer();
    return placer.w(widget.getWidth()).h(widget.getHeight()).x(widget.getX()).y(widget.getY());
  }

  public void setPrefix(String prefix) {
    prefix = prefix.trim();
    if (!prefix.isEmpty() && prefix.charAt(prefix.length() - 1) != '.') {
      prefix += ".";
    }
    this.prefix = prefix;
  }

  public int textStrW(String str) {
    return graphics.getWidth(str);
  }

  public int textW(String keyName) {
    return textW(text(keyName));
  }

  public int textW(Component text) {
    return graphics.getWidth(text);
  }

  public Collection<Component> texts(Iterable<String> strs) {
    return stream(strs).map(str -> (Component) text(str)).toList();
  }

  public int maxTextStrW(Iterable<String> texts) {
    return stream(texts).map(this::textStrW).max(Integer::compare).orElse(0);
  }

  public Collection<Component> texts(String keyName, String... others) {
    return texts(Lists.asList(keyName, others));
  }

  public int maxTextW(Iterable<Component> texts) {
    return stream(texts).map(this::textW).max(Integer::compare).orElse(0);
  }

  public int maxTextW(Component first, Component... others) {
    return maxTextW(Lists.asList(first, others));
  }

  public int buttonW(String text) {
    return buttonW(text(text));
  }

  public int buttonW(Component text) {
    return textW(text) + 2 * enW;
  }

  public MutableComponent text(String keyName) {
    if (!prefix.isEmpty()) {
      keyName = prefix + keyName;
    }
    return Component.translatable(keyName);
  }

  public MutableComponent literal(String str) {
    return Component.literal(str);
  }

  public Tooltip tooltip(String keyName) {
    return Tooltip.create(text(keyName));
  }

  public int buttonW(Iterable<Component> texts) {
    int maxW = 0;
    for (Component t : texts) {
      maxW = Math.max(buttonW(t), maxW);
    }
    return maxW;
  }

  public int onOffButtonW(Component text) {
    int onOffW = buttonW(
        List.of(Component.translatable("options.on"), Component.translatable("options.off")));
    return textW(text) + textW(Component.literal(": ")) + onOffW;
  }

  public void drawBackground(GuiGraphicsExtractor extractor, Identifier texture, float delta, int mouseX, int mouseY) {
    int x = (graphics.getWindowW() - graphics.getScreenW()) / 2;
    int y = (graphics.getWindowH() - graphics.getScreenH()) / 2;
    int w = graphics.getScreenW();
    int h = graphics.getScreenH();
    extractor.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0f, 0f, w, h, w, h);
  }

  public void drawText(GuiGraphicsExtractor extractor, net.minecraft.client.gui.Font font, Placer placer,
      Component text, int color) {
    extractor.text(font, text, placer.relX(), placer.relY(), color, false);
  }

  public <T extends GuiEventListener & Renderable & NarratableEntry> T addRenderableWidget(T element) {
    return graphics.addRenderableWidget(element);
  }

  public Placer screenPlace() {
    return placer().size(screenW, screenH).at(screenX, screenY);
  }
}
