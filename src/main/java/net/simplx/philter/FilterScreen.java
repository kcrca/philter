package net.simplx.philter;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.Arrays.stream;
import static net.simplx.philter.FilterDesc.MATCHES_MAX_COUNT;
import static net.simplx.philter.FilterMode.MATCHES;
import static net.simplx.philter.FilterMode.NONE;
import static net.simplx.philter.FilterMode.values;
import static net.simplx.philter.PhilterMod.MOD_ID;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
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

  // EditBox draws the max-char display _outside_ itself, this gives room for that.
  private static final int MATCHES_MAX_CHAR_H = TEXT_H * 3 / 2;
  private static final int MATCHES_X = MODE_X;
  private static final int MATCHES_Y = MODE_Y + MODE_H + BORDER;
  private static final int MATCHES_W = SCREEN_W - MATCHES_X - BORDER;
  private static final int MATCHES_H = SCREEN_H - MATCHES_Y - BORDER - MATCHES_MAX_CHAR_H;

  private static final int SAVE_X = MODE_X;
  private static final int SAVE_Y = MATCHES_Y + MATCHES_H + MATCHES_MAX_CHAR_H - BUTTON_H;

  private static final Pattern RESOURCE_PAT = Pattern.compile("!?#[-a-z0-9_./]+");

  private final FilterDesc desc;

  private Text filterTitle;
  private CyclingButtonWidget<Boolean> exactButton;
  private TextFieldWidget[] matchesFields;
  private ButtonWidget saveButton;

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
    super.init();

    TextHelper th = new TextHelper();

    // Calculate the text- and font-relative values.
    Text saveText = th.text("philter.filter.save");
    Text exactText = th.text("philter.filter.exact");
    filterTitle = th.text("philter.filter.name").append(":");

    int filterTitleW = th.textW(filterTitle) + textRenderer.getWidth(" ");
    int modeW = th.buttonW(stream(values()).map(
            mode -> th.text("philter.filter.mode." + mode.toString().toLowerCase()))
        .collect(Collectors.toList()));
    int saveButtonW = th.buttonW(saveText);

    int modeX = x + MODE_X + filterTitleW;
    addDrawableChild(
        CyclingButtonWidget.builder(FilterScreen::filterText).values(values()).omitKeyText()
            .initially(desc.mode).tooltip(value -> th.tooltip(
                "philter.filter.mode." + value.toString().toLowerCase() + ".tooltip"))
            .build(modeX, y + MODE_Y, modeW, BUTTON_H, null, (button, mode) -> setMode(mode)));

    int exactW = th.onOffButtonW(exactText);
    int exactX = backgroundWidth - exactW - BORDER;
    exactButton = addDrawableChild(CyclingButtonWidget.onOffBuilder(desc.exact)
        .tooltip(value -> th.tooltip("philter.filter.exact." + value + ".tooltip"))
        .build(x + exactX, y + EXACT_Y, exactW, BUTTON_H, exactText,
            (button, exact) -> setExact(exact)));
    Text matchesAltText = th.text("philter.filter_mode.matches_alt");
    Tooltip matchTooltip = th.tooltip("philter.filter.mode.matches.tooltip");
    matchesFields = new TextFieldWidget[MATCHES_MAX_COUNT];
    for (int i = 0; i < MATCHES_MAX_COUNT; i++) {
      int row = i % (MATCHES_MAX_COUNT / 2);
      int col = i / (MATCHES_MAX_COUNT / 2);
      var field = matchesFields[i] = addDrawableChild(
          new TextFieldWidget(textRenderer, x + MATCHES_X + col * MATCHES_W / 2,
              y + MATCHES_Y + TEXT_H * row, MATCHES_W / 2, TEXT_H, th.text("")));
      final int index = i;
      field.setChangedListener(text -> matchChanged(index, text));
      // Set here instead of on creation, so all text is handled through the same change mechansim.
      field.setText(i < desc.matches.size() ? desc.matches.get(i) : "");
      field.setTooltip(matchTooltip);
      field.visible = false;
    }

    // TODO: Add all/any button
    saveButton = addDrawableChild(
        new ButtonWidget.Builder(saveText, this::save).dimensions(this.x + SAVE_X, y + SAVE_Y,
            saveButtonW, BUTTON_H).tooltip(th.tooltip("philter.save.tooltip")).build());

    setMatchesVisible(false); // ... so if it needs to be visible, it will be newly visible.
    reactToChange();
  }

  @Override
  protected void drawForeground(MatrixStack matrices, int mouseX, int mouseY) {
    super.drawForeground(matrices, mouseX, mouseY);
    textRenderer.draw(matrices, filterTitle, MODE_X, MODE_Y + (BUTTON_H - TEXT_H) / 2.0f,
        TITLE_TEXT_COLOR);
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
    if (spec.isEmpty() || RESOURCE_PAT.matcher(spec).matches()) {
      return;
    }
    try {
      field.setTooltip(null);
      Pattern.compile(spec);
      return;
    } catch (PatternSyntaxException e) {
      field.setTooltip(Tooltip.of(Text.literal(e.getMessage())));
    }
    field.setEditableColor(0xff0000);
    reactToChange();
  }

  private static Text filterText(FilterMode value) {
    return Text.translatable("philter.filter.mode." + value.asString());
  }

  private void setMode(FilterMode mode) {
    desc.mode = mode;
    reactToChange();
  }

  private void setExact(boolean exact) {
    desc.exact = exact;
    sendFilterDesc();
  }

  private void reactToChange() {
    exactButton.visible = desc.mode != NONE;
    saveButton.visible = anyMatchChanged();

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
    for (int i = 0; i < matchesFields.length; i++) {
      String orig = i < desc.matches.size() ? desc.matches.get(i) : "";
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
