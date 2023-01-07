package net.simplx.philter;

import static net.simplx.philter.FilterMode.MATCHES;
import static net.simplx.philter.PhilterMod.MOD_ID;

import com.mojang.blaze3d.systems.RenderSystem;
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

@Environment(EnvType.CLIENT)
public class FilterScreen extends HandledScreen<FilterScreenHandler> {

  private static final Identifier TEXTURE = new Identifier(MOD_ID,
      "textures/gui/container/filter.png");

  private static final int TITLE_TEXT_COLOR = 4210752; // A constant in minecraft source...somewhere?

  private static final int SCREEN_HEIGHT = 233;
  private static final int TEXT_HEIGHT = MinecraftClient.getInstance().textRenderer.fontHeight + 2;
  private static final int BUTTON_HEIGHT = TEXT_HEIGHT + 4;
  private static final int INVENTORY_BOTTOM = 130;
  private static final int BORDER = 8;

  private static final int FILTER_Y = INVENTORY_BOTTOM + TEXT_HEIGHT / 2;

  private static final int MATCHES_LIST_X = BORDER;
  private static final int MATCHES_LIST_Y = FILTER_Y + BUTTON_HEIGHT;

  private static final int SAVE_X = BORDER;
  private static final int SAVE_Y = SCREEN_HEIGHT - BORDER - BUTTON_HEIGHT;

  private final FilterDesc desc;
  private EditBoxWidget editBox;
  private ButtonWidget saveButton;

  private Text filterTitle;

  public FilterScreen(FilterScreenHandler handler, PlayerInventory inventory, Text title) {
    super(handler, inventory, title);
    passEvents = false;
    backgroundHeight = SCREEN_HEIGHT;
    playerInventoryTitleY = backgroundHeight - 194;
    desc = handler.getFilterDesc();
  }

  protected void init() {
    super.init();

    // Calculate the text- and font-relative values.
    Text saveText = Text.translatable("philter.save");
    filterTitle = Text.translatable("philter.filter.name").append(":");

    int spaceWidth = textRenderer.getWidth(" ");
    int textWidth = textRenderer.getWidth(filterTitle) + spaceWidth;
    int maxWidth = 0;
    for (FilterMode mode : FilterMode.values()) {
      maxWidth = Math.max(maxWidth, textRenderer.getWidth(mode.asString()));
    }
    int filterButtonWidth = maxWidth + spaceWidth * 2;
    int saveButtonWidth = textRenderer.getWidth(saveText) + 2 * spaceWidth;

    addDrawableChild(
        CyclingButtonWidget.builder(FilterScreen::filterText).values(FilterMode.values())
            .omitKeyText().initially(desc.mode)
            .build(x + BORDER + textWidth, FILTER_Y, filterButtonWidth, BUTTON_HEIGHT, null,
                (button, mode) -> setMode(mode)));

    MutableText matchesAltText = Text.translatable("philter.filter_mode.matches_alt");
    editBox = addDrawableChild(
        new EditBoxWidget(textRenderer, x + MATCHES_LIST_X, y + MATCHES_LIST_Y,
            backgroundWidth - 2 * BORDER, SCREEN_HEIGHT - MATCHES_LIST_Y - 2 * BORDER - TEXT_HEIGHT,
            matchesAltText, matchesAltText));
    editBox.setMaxLength(FilterDesc.MATCHES_MAX_LEN);
    editBox.setText(desc.matchSpec);
    editBox.setChangeListener(this::updateForSpec);

    saveButton = addDrawableChild(
        new ButtonWidget.Builder(saveText, this::save).dimensions(x + SAVE_X, y + SAVE_Y,
            saveButtonWidth, BUTTON_HEIGHT).build());

    updateForSpec(desc.matchSpec);
    updateForMode();
  }

  @Override
  protected void drawForeground(MatrixStack matrices, int mouseX, int mouseY) {
    super.drawForeground(matrices, mouseX, mouseY);
    textRenderer.draw(matrices, filterTitle, BORDER,
        FILTER_Y + (BUTTON_HEIGHT - TEXT_HEIGHT) / 2.0f, TITLE_TEXT_COLOR);
  }

  private void updateForSpec(String text) {
    saveButton.visible = !text.equals(desc.matchSpec) && editBox.visible;
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
    editBox.visible = desc.mode == MATCHES;
  }

  private void storeText() {
    desc.matchSpec = editBox.getText();
    updateForSpec(desc.matchSpec);
  }

  private void save(ButtonWidget unused) {
    storeText();
    sendFilterDesc();
  }

  private void sendFilterDesc() {
    client.player.networkHandler.sendPacket(new FilterPacket(desc, handler.getPos()));
  }

  public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
    renderBackground(matrices);
    super.render(matrices, mouseX, mouseY, delta);
    drawMouseoverTooltip(matrices, mouseX, mouseY);
  }

  @Override
  public void close() {
    if (editBox != null && !editBox.getText().equals(desc.matchSpec)) {
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
    drawTexture(matrices, midX, midY, 0, 0, backgroundWidth, backgroundHeight);
  }
}
