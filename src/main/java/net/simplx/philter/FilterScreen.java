package net.simplx.philter;

import static net.simplx.philter.FilterMode.MATCHES;
import static net.simplx.philter.PhilterMod.MOD_ID;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.EditBoxWidget;
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

  private static final int GENERAL_Y = MODE_Y;

  // EditBox draws the max-char display _outside_ itself, this gives room for that.
  private static final int MATCHES_MAX_CHAR_H = TEXT_H * 3 / 2;
  private static final int MATCHES_X = MODE_X;
  private static final int MATCHES_Y = MODE_Y + MODE_H + BORDER;
  private static final int MATCHES_W = SCREEN_W - MATCHES_X - BORDER;
  private static final int MATCHES_H = SCREEN_H - MATCHES_Y - BORDER - MATCHES_MAX_CHAR_H;

  private static final int SAVE_X = MODE_X;
  private static final int SAVE_Y = MATCHES_Y + MATCHES_H + MATCHES_MAX_CHAR_H - BUTTON_H;

  private final FilterDesc desc;
  private EditBoxWidget matchesBox;
  private ButtonWidget saveButton;

  private Text filterTitle;

  public FilterScreen(FilterScreenHandler handler, PlayerInventory inventory, Text title) {
    super(handler, inventory, title);
    passEvents = false;
    backgroundHeight = SCREEN_H;
    backgroundWidth = SCREEN_W;
    playerInventoryTitleY = backgroundHeight - 94;
    desc = handler.getFilterDesc();
  }

  protected void init() {
    super.init();

    // Calculate the text- and font-relative values.
    Text saveText = Text.translatable("philter.save");
    Text exactText = Text.translatable("philter.exact");
    Text generalText = Text.translatable("philter.general");
    filterTitle = Text.translatable("philter.filter.name").append(":");

    int enW = textRenderer.getWidth("n");
    int filterTitleW = textW(filterTitle) + textRenderer.getWidth(" ");
    int modeW = buttonW(Arrays.stream(FilterMode.values())
        .map(mode -> Text.translatable("philter.filter.mode." + mode.toString().toLowerCase()))
        .collect(Collectors.toList()), enW);
    int saveButtonW = buttonW(saveText, enW);
    int exactButtonW = buttonW(exactText, enW);

    int modeX = x + MODE_X + filterTitleW;
    addDrawableChild(
        CyclingButtonWidget.builder(FilterScreen::filterText).values(FilterMode.values())
            .omitKeyText().initially(desc.mode)
            .build(modeX, y + MODE_Y, modeW, BUTTON_H, null, (button, mode) -> setMode(mode)));

    int generalW = onOffButtonW(exactText, enW);
    int generalX = backgroundWidth - generalW - BORDER;
    addDrawableChild(CyclingButtonWidget.onOffBuilder(true)
        .build(x + generalX, y + GENERAL_Y, generalW, BUTTON_H, exactText,
            (button, general) -> setGeneral(general)));
    MutableText matchesAltText = Text.translatable("philter.filter_mode.matches_alt");
    matchesBox = addDrawableChild(
        new EditBoxWidget(textRenderer, this.x + MATCHES_X, y + MATCHES_Y, MATCHES_W, MATCHES_H,
            matchesAltText, matchesAltText));
    matchesBox.setMaxLength(FilterDesc.MATCHES_MAX_LEN);
    matchesBox.setText(desc.matchSpec);
    matchesBox.setChangeListener(this::updateForSpec);

    saveButton = addDrawableChild(
        new ButtonWidget.Builder(saveText, this::save).dimensions(this.x + SAVE_X, y + SAVE_Y,
            saveButtonW, BUTTON_H).build());

    updateForSpec(desc.matchSpec);
    updateForMode();
  }

  private void setGeneral(boolean general) {
  }

  private int textW(Text text) {
    return textRenderer.getWidth(text);
  }

  private int buttonW(Text text, int enW) {
    return textW(text) + 2 * enW;
  }

  private int buttonW(Iterable<Text> texts, int enW) {
    int maxW = 0;
    for (Text t : texts) {
      maxW = Math.max(buttonW(t, enW), maxW);
    }
    return maxW;
  }

  private int onOffButtonW(Text text, int enW) {
    int onOffW = buttonW(List.of(Text.translatable("options.on"), Text.translatable("options.off")),
        enW);
    return textW(text) + textW(Text.literal(": ")) + onOffW;
  }

  @Override
  protected void drawForeground(MatrixStack matrices, int mouseX, int mouseY) {
    super.drawForeground(matrices, mouseX, mouseY);
    textRenderer.draw(matrices, filterTitle, MODE_X, MODE_Y + (BUTTON_H - TEXT_H) / 2.0f,
        TITLE_TEXT_COLOR);
  }

  private void updateForSpec(String text) {
    saveButton.visible = !text.equals(desc.matchSpec) && matchesBox.visible;
  }

  private static Text filterText(FilterMode value) {
    return Text.translatable("philter.filter.mode." + value.asString());
  }

  private void setMode(FilterMode mode) {
    desc.mode = mode;
    updateForMode();
    sendFilterDesc();
  }

  private void updateForMode() {
    matchesBox.visible = desc.mode == MATCHES;
  }

  private void storeText() {
    desc.matchSpec = matchesBox.getText();
    updateForSpec(desc.matchSpec);
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
    if (matchesBox != null && !matchesBox.getText().equals(desc.matchSpec)) {
      storeText();
      sendFilterDesc();
    }
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

  private void drawBox(MatrixStack matrices, int x, int y, int width, int height, int color) {
    drawHorizontalLine(matrices, x, x + width, y, color);
    drawHorizontalLine(matrices, x, x + width, y + height, color);
    drawVerticalLine(matrices, x, y, y + height, color);
    drawVerticalLine(matrices, x + width, y, y + height, color);
  }
}
