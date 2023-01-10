package net.simplx.philter;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.Arrays.stream;
import static net.simplx.philter.FilterMode.MATCHES;
import static net.simplx.philter.FilterMode.NONE;
import static net.simplx.philter.FilterMode.values;
import static net.simplx.philter.PhilterMod.MOD_ID;
import static net.simplx.philter.layout.Horizontal.LEFT;
import static net.simplx.philter.layout.Horizontal.RIGHT;
import static net.simplx.philter.layout.Vertical.ABOVE;
import static net.simplx.philter.layout.Vertical.BELOW;
import static net.simplx.philter.layout.Vertical.MID;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.simplx.philter.layout.Layout;
import net.simplx.philter.layout.Layout.Placer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("SameParameterValue")
@Environment(EnvType.CLIENT)
public class FilterScreen extends HandledScreen<FilterScreenHandler> {

  private static final Logger LOGGER = LoggerFactory.getLogger(FilterScreen.class);

  private static final Identifier TEXTURE = new Identifier(MOD_ID,
      "textures/gui/container/filter.png");

  private static final int TITLE_TEXT_COLOR = 4210752; // A constant in minecraft source...somewhere?

  private static final int SCREEN_H = 133;
  private static final int SCREEN_W = 346;
  private static final int TEXT_H = MinecraftClient.getInstance().textRenderer.fontHeight + 2;
  private static final int BUTTON_H = TEXT_H + 4;
  private static final int BORDER = 8;

  private static final int MODE_X = 176;
  private static final int MODE_Y = BORDER;
  private static final int MODE_H = BUTTON_H;

  private static final int EXACT_Y = MODE_Y;

  private static final int ALL_X = MODE_X;
  private static final int ALL_Y = MODE_Y + MODE_H + BORDER;

  private static final int SAVE_Y = ALL_Y;

  private static final int MATCHES_X = MODE_X;
  private static final int MATCHES_Y = ALL_Y + BUTTON_H + BORDER;
  private static final int MATCHES_W = SCREEN_W - MATCHES_X - BORDER;

  private static final Pattern RESOURCE_PAT = Pattern.compile("!?#[-a-z0-9_./]+");

  private final FilterDesc desc;

  private MutableText filterTitle;
  private CyclingButtonWidget<Boolean> exactButton;
  private TextFieldWidget[] matchesFields;
  private CyclingButtonWidget<Boolean> allButton;
  private ButtonWidget saveButton;
  private Tooltip matchTooltip;
  private boolean initializing;
  private Placer titlePlace;
  private Layout layout;

  public FilterScreen(FilterScreenHandler handler, PlayerInventory inventory, Text title) {
    super(handler, inventory, title);
    passEvents = false;
    backgroundHeight = SCREEN_H;
    backgroundWidth = SCREEN_W;
    playerInventoryTitleY = backgroundHeight - 94;
    desc = handler.getFilterDesc();
  }

  class TextHelper {

    Tooltip tooltip(String keyName) {
      return Tooltip.of(text(keyName));
    }

    MutableText text(String keyName) {
      return Text.translatable(keyName);
    }

    final int enW = textRenderer.getWidth("n");

    int textW(Text text) {
      return textRenderer.getWidth(text);
    }

    int buttonW(Text text) {
      return textW(text) + 2 * enW;
    }

    int buttonW(Iterable<Text> texts) {
      int maxW = 0;
      for (Text t : texts) {
        maxW = Math.max(buttonW(t), maxW);
      }
      return maxW;
    }

    int onOffButtonW(Text text) {
      int onOffW = buttonW(
          List.of(Text.translatable("options.on"), Text.translatable("options.off")));
      return textW(text) + textW(Text.literal(": ")) + onOffW;
    }
  }

  protected void init() {
    initializing = true;
    super.init();

    layout = new Layout(this, SCREEN_W, SCREEN_H);
    layout.setPrefix("philter.filter");

    // Calculate the text- and font-relative values.
    Text saveText = layout.text("save");
    filterTitle = layout.text("name").append(":");

    titlePlace = layout.placer().w(layout.textW(filterTitle)).h(layout.textH).relX(MODE_X)
        .align(ABOVE);

    Placer p;
    List<Text> modeTexts = stream(values()).map(
        mode -> (Text) layout.text("mode." + mode.toString().toLowerCase())).toList();
    p = layout.placer().withTexts(modeTexts).inButton().align(RIGHT, titlePlace)
        .align(MID, titlePlace);
    Iterator<Text> modeIt = modeTexts.iterator();
    var modeButton = addDrawableChild(
        CyclingButtonWidget.builder(which -> modeIt.next()).values(values()).omitKeyText()
            .initially(desc.mode)
            .tooltip(value -> layout.tooltip("mode." + value.toString().toLowerCase() + ".tooltip"))
            .build(p.x(), p.y(), p.w(), p.h(), null, (button, m) -> setMode((FilterMode) m)));

    Text allText = layout.text("all");
    Text anyText = layout.text("any");
    p = layout.placer().withTexts(anyText, allText).inButton().align(RIGHT, modeButton)
        .align(MID, titlePlace);
    allButton = addDrawableChild(
        CyclingButtonWidget.onOffBuilder(anyText, allText).initially(true).omitKeyText()
            .build(p.x(), p.y(), p.w(), p.h(), null, (button, all) -> setAll(all)));

    Text exactText = layout.text("exact");
    p = layout.placer().w(layout.onOffButtonW(exactText)).h(layout.textH).inButton()
        .x(titlePlace.x()).align(BELOW, modeButton);
    exactButton = addDrawableChild(CyclingButtonWidget.onOffBuilder(desc.exact)
        .tooltip(value -> layout.tooltip("exact." + value + ".tooltip"))
        .build(p.x(), p.y(), p.w(), p.h(), exactText, (button, exact) -> setExact(exact)));

    p = layout.placer().withText(saveText).inButton().align(RIGHT).align(MID, exactButton);
    saveButton = addDrawableChild(
        new ButtonWidget.Builder(saveText, this::save).dimensions(p.x(), p.y(), p.w(), p.h())
            .tooltip(layout.tooltip("tooltip")).build());

    // addDrawableChild(
    //     CyclingButtonWidget.builder((FilterMode value1) -> ly.text("mode." + value1.asString()))
    //
    //         .values(values()).omitKeyText().initially(desc.mode)
    //         .tooltip(value -> ly.tooltip("mode." + value.toString().toLowerCase() + ".tooltip"))
    //         .build(modeX, y + MODE_Y, modeW, BUTTON_H, null, (button, mode) -> setMode(mode)));
    //
    // matchesFields = new TextFieldWidget[MATCHES_MAX_COUNT];
    // boolean foundFocus = false;
    // for (int i = 0; i < MATCHES_MAX_COUNT; i++) {
    //   int col = i % 2;
    //   int row = i / 2;
    //   var field = matchesFields[i] = addDrawableChild(
    //       new TextFieldWidget(textRenderer, x + MATCHES_X + col * MATCHES_W / 2,
    //           y + MATCHES_Y + TEXT_H * row, MATCHES_W / 2, TEXT_H, ly.text("")));
    //   final int index = i;
    //   field.setChangedListener(text -> matchChanged(index, text));
    //   // Set here instead of on creation, so all text is handled through the same change mechansim.
    //   String match = desc.match(i);
    //   field.setText(match);
    //   if (!foundFocus && (match.isEmpty() || i == MATCHES_MAX_COUNT - 1)) {
    //     foundFocus = true;
    //     setInitialFocus(field);
    //   }
    //   field.visible = false;
    // }
    // ;
    //
    // int saveX = SCREEN_W - BORDER - ly.buttonW(saveText);
    //
    // setMatchesVisible(false); // ... so if it needs to be visible, it will be newly visible.
    //
    // initializing = false;
    // reactToChange();
  }

  @Override
  protected void drawForeground(MatrixStack matrices, int mouseX, int mouseY) {
    super.drawForeground(matrices, mouseX, mouseY);
    layout.drawText(matrices, titlePlace, filterTitle, TITLE_TEXT_COLOR);
  }

  private void matchChanged(int i, String text) {
    String spec = text.trim();
    TextFieldWidget field = matchesFields[i];
    if (!spec.equals(text)) {
      // This will cause recursion back into here, so just return.
      field.setText(spec);
      return;
    }

    // Color the text based on validity.
    field.setEditableColor(0xffffff);
    field.setTooltip(matchTooltip);
    if (!spec.isEmpty() && !RESOURCE_PAT.matcher(spec).matches()) {
      try {
        Pattern.compile(spec);
      } catch (PatternSyntaxException e) {
        field.setTooltip(Tooltip.of(Text.literal(e.getMessage())));
        field.setEditableColor(0xff0000);
      }
    }
    reactToChange();
  }

  private void setMode(FilterMode mode) {
    desc.mode = mode;
    reactToChange();
  }

  private void setExact(boolean exact) {
    desc.exact = exact;
    sendFilterDesc();
  }

  private void setAll(Boolean all) {
    desc.matchAll = all;
    sendFilterDesc();
  }

  private void reactToChange() {
    if (initializing) {
      return;
    }
    exactButton.visible = desc.mode != NONE;
    allButton.visible = desc.mode == MATCHES;
    saveButton.visible = allButton.visible && anyMatchChanged();

    // We have to deal with focus before changing visibility
    var wasVisible = matchesFields[0].visible;
    boolean newVisible = desc.mode == MATCHES;
    if (!wasVisible && newVisible) {
      setInitialFocus(matchesFields[0]);
    } else if (wasVisible && !newVisible) {
      if (stream(matchesFields).anyMatch(ClickableWidget::isFocused)) {
        TextFieldWidget lastField = matchesFields[matchesFields.length - 1];
        setFocused(lastField);
        lastField.changeFocus(true);
      }
    }
    setMatchesVisible(newVisible);

    sendFilterDesc();
  }

  private boolean anyMatchChanged() {
    if (matchesFields == null) {
      return false;
    }
    for (int i = 0; i < matchesFields.length; i++) {
      String orig = desc.match(i);
      if (!matchesFields[i].getText().equals(orig)) {
        return true;
      }
    }
    return false;
  }

  private void setMatchesVisible(boolean newVisible) {
    stream(matchesFields).forEach(field -> field.visible = newVisible);
  }

  private void storeText() {
    desc.matches = stream(matchesFields).map(TextFieldWidget::getText)
        .filter(text -> !text.isBlank()).collect(toImmutableList());
  }

  private void save(ButtonWidget unused) {
    storeText();
    sendFilterDesc();
    reactToChange();
  }

  private void sendFilterDesc() {
    try {
      //noinspection ConstantConditions
      client.player.networkHandler.sendPacket(new FilterPacket(desc, handler.getPos()));
    } catch (NullPointerException e) {
      LOGGER.error("Unexpected null", e);
    }
  }

  public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
    renderBackground(matrices);
    super.render(matrices, mouseX, mouseY, delta);
    drawMouseoverTooltip(matrices, mouseX, mouseY);
  }

  @Override
  public void close() {
    if (anyMatchChanged()) {
      storeText();
    }
    sendFilterDesc();
    super.close();
  }

  protected void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY) {
    RenderSystem.setShader(GameRenderer::getPositionTexProgram);
    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    RenderSystem.setShaderTexture(0, TEXTURE);
    int midX = (width - backgroundWidth) / 2;
    int midY = (height - backgroundHeight) / 2;
    drawTexture(matrices, midX, midY, 0, 0, backgroundWidth, backgroundHeight, 512, 256);
  }

  @SuppressWarnings("unused")
  private void drawBox(MatrixStack matrices, int x, int y, int width, int height, int color) {
    drawHorizontalLine(matrices, x, x + width, y, color);
    drawHorizontalLine(matrices, x, x + width, y + height, color);
    drawVerticalLine(matrices, x, y, y + height, color);
    drawVerticalLine(matrices, x + width, y, y + height, color);
  }
}
