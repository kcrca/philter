package net.simplx.philter;

import static net.simplx.philter.FilterMode.MATCHES;
import static net.simplx.philter.FilterMode.NONE;
import static net.simplx.philter.FilterMode.values;
import static net.simplx.philter.PhilterMod.MOD_ID;

import com.mojang.blaze3d.systems.RenderSystem;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.EditBoxWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.InputUtil;
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

  private final FilterDesc desc;
  private EditBoxWidget matchesBox;
  private ButtonWidget saveButton;

  private Text filterTitle;
  private CyclingButtonWidget<Boolean> exactButton;

  /**
   * This class exists because EditBoxWidget does _not_ work around the fact that Screen will
   * automatically close itself if the "inventory" key is pressed. So if the user types 'e' (or
   * whatever) in their matches, the window goes away. This is the most direct way around this bug,
   * and it ought to be fixed, but until then, we work around it by unbinding the inventory key,
   * handling the key press, and then rebinding it.
   */
  private class HackedEditBoxWidget extends EditBoxWidget implements Forcer {

    private static final Field BOUND_KEY_F = new StaticForcer(KeyBinding.class).field("boundKey");

    public HackedEditBoxWidget(TextRenderer textRenderer, int x, int y, int width, int height,
        Text placeholder, Text message) {
      super(textRenderer, x, y, width, height, placeholder, message);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (active && FilterScreen.this.client.options.inventoryKey.matchesKey(keyCode, scanCode)) {
        return true;
      }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
  }

  public FilterScreen(FilterScreenHandler handler, PlayerInventory inventory, Text title) {
    super(handler, inventory, title);
    passEvents = false;
    backgroundHeight = SCREEN_H;
    backgroundWidth = SCREEN_W;
    playerInventoryTitleY = backgroundHeight - 94;
    desc = handler.getFilterDesc();
  }

  class TextHelper {

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

    // If I don't do this, whenever the user types an 'e' (or whatever) into the text box,
    // the whole window will close.
    // this.client.options.inventoryKey

    // Calculate the text- and font-relative values.
    Text saveText = Text.translatable("philter.save");
    Text exactText = Text.translatable("philter.exact");
    filterTitle = Text.translatable("philter.filter.name").append(":");

    TextHelper th = new TextHelper();
    int filterTitleW = th.textW(filterTitle) + textRenderer.getWidth(" ");
    int modeW = th.buttonW(Arrays.stream(values())
        .map(mode -> Text.translatable("philter.filter.mode." + mode.toString().toLowerCase()))
        .collect(Collectors.toList()));
    int saveButtonW = th.buttonW(saveText);

    int modeX = x + MODE_X + filterTitleW;
    addDrawableChild(
        CyclingButtonWidget.builder(FilterScreen::filterText).values(values()).omitKeyText()
            .initially(desc.mode)
            .build(modeX, y + MODE_Y, modeW, BUTTON_H, null, (button, mode) -> setMode(mode)));

    int exactW = th.onOffButtonW(exactText);
    int exactX = backgroundWidth - exactW - BORDER;
    exactButton = addDrawableChild(CyclingButtonWidget.onOffBuilder(desc.exact)
        .build(x + exactX, y + EXACT_Y, exactW, BUTTON_H, exactText,
            (button, exact) -> setExact(exact)));
    MutableText matchesAltText = Text.translatable("philter.filter_mode.matches_alt");
    matchesBox = addDrawableChild(
        new HackedEditBoxWidget(textRenderer, this.x + MATCHES_X, y + MATCHES_Y, MATCHES_W,
            MATCHES_H, matchesAltText, matchesAltText));
    matchesBox.setMaxLength(FilterDesc.MATCHES_MAX_LEN);
    matchesBox.setText(desc.matches);
    matchesBox.setChangeListener(this::matchesChanged);

    saveButton = addDrawableChild(
        new ButtonWidget.Builder(saveText, this::save).dimensions(this.x + SAVE_X, y + SAVE_Y,
            saveButtonW, BUTTON_H).build());

    matchesChanged(desc.matches);
    reactToChange();
  }

  @Override
  protected void drawForeground(MatrixStack matrices, int mouseX, int mouseY) {
    super.drawForeground(matrices, mouseX, mouseY);
    textRenderer.draw(matrices, filterTitle, MODE_X, MODE_Y + (BUTTON_H - TEXT_H) / 2.0f,
        TITLE_TEXT_COLOR);
  }

  private void matchesChanged(String text) {
    saveButton.visible = !text.equals(desc.matches) && matchesBox.visible;
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
    matchesBox.visible = desc.mode == MATCHES;
    exactButton.visible = desc.mode != NONE;
    sendFilterDesc();
  }

  private void storeText() {
    desc.matches = matchesBox.getText();
    matchesChanged(desc.matches);
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
    if (matchesBox != null && !matchesBox.getText().equals(desc.matches)) {
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

  @SuppressWarnings("unused")
  private void drawBox(MatrixStack matrices, int x, int y, int width, int height, int color) {
    drawHorizontalLine(matrices, x, x + width, y, color);
    drawHorizontalLine(matrices, x, x + width, y + height, color);
    drawVerticalLine(matrices, x, y, y + height, color);
    drawVerticalLine(matrices, x + width, y, y + height, color);
  }
}
