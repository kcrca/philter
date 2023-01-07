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
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class FilterScreen extends HandledScreen<FilterScreenHandler> {

  private static final Identifier TEXTURE = new Identifier(MOD_ID,
      "textures/gui/container/filter.png");

  private static final int TITLE_TEXT_COLOR = 4210752; // A constant in minecraft source...somewhere?

  private static final int SCREEN_HEIGHT = 233;
  private static final int TEXT_HEIGHT = MinecraftClient.getInstance().textRenderer.fontHeight + 2;
  private static final int BUTTON_HEIGHT = TEXT_HEIGHT + 2;
  private static final int INVENTORY_BOTTOM = 130;
  private static final int BORDER = 8;
  private static final int SPACE_WIDTH;

  private static final Text FILTER_TITLE = Text.translatable("philter.filter.name").append(":");
  private static final int FILTER_Y = INVENTORY_BOTTOM + TEXT_HEIGHT / 2;
  private static final int FILTER_BUTTON_WIDTH;

  private static final int MATCHES_LIST_X = BORDER;
  private static final int MATCHES_LIST_Y = FILTER_Y + BUTTON_HEIGHT;

  private static final Text SAVE_TEXT = Text.translatable("philter.save");
  private static final int SAVE_X = BORDER;
  private static final int SAVE_Y = SCREEN_HEIGHT - BORDER - BUTTON_HEIGHT;

  static {
    var renderer = MinecraftClient.getInstance().textRenderer;
    var maxWidth = 0;
    for (FilterMode mode : FilterMode.values()) {
      maxWidth = Math.max(maxWidth, renderer.getWidth(mode.asString()));
    }
    // 2: Space on either side
    SPACE_WIDTH = renderer.getWidth(" ");
    FILTER_BUTTON_WIDTH = maxWidth + SPACE_WIDTH * 2;
  }

  private final FilterDesc desc;
  private EditBoxWidget editBox;
  private ButtonWidget saveButton;

  public FilterScreen(FilterScreenHandler handler, PlayerInventory inventory, Text title) {
    super(handler, inventory, title);
    passEvents = false;
    backgroundHeight = SCREEN_HEIGHT;
    playerInventoryTitleY = backgroundHeight - 194;
    desc = handler.getFilterDesc();
  }

  @Override
  protected void drawForeground(MatrixStack matrices, int mouseX, int mouseY) {
    super.drawForeground(matrices, mouseX, mouseY);
    textRenderer.draw(matrices, FILTER_TITLE, BORDER,
        FILTER_Y + (BUTTON_HEIGHT - TEXT_HEIGHT) / 2.0f, TITLE_TEXT_COLOR);
  }

  protected void init() {
    super.init();
    var textWidth = textRenderer.getWidth(FILTER_TITLE) + textRenderer.getWidth(" ");

    addDrawableChild(
        CyclingButtonWidget.builder(FilterScreen::filterText).values(FilterMode.values())
            .omitKeyText().initially(desc.mode)
            .build(x + BORDER + textWidth, FILTER_Y, FILTER_BUTTON_WIDTH, BUTTON_HEIGHT, null,
                (button, mode) -> setMode(mode)));

    editBox = addDrawableChild(
        new EditBoxWidget(client.textRenderer, x + MATCHES_LIST_X, y + MATCHES_LIST_Y,
            backgroundWidth - 2 * BORDER,
            SCREEN_HEIGHT - MATCHES_LIST_Y - 2 * BORDER - BUTTON_HEIGHT,
            Text.literal(desc.matchSpec), Text.literal("message")));
    editBox.setMaxLength(FilterDesc.MATCHES_MAX_LEN);
    editBox.setChangeListener(this::updateForSpec);

    int saveWidth =
        MinecraftClient.getInstance().textRenderer.getWidth(SAVE_TEXT) + 2 * SPACE_WIDTH;
    saveButton = addDrawableChild(
        new ButtonWidget.Builder(SAVE_TEXT, this::save).dimensions(x + SAVE_X, y + SAVE_Y,
            saveWidth, BUTTON_HEIGHT).build());

    updateForMode();
    updateForSpec(desc.matchSpec);
  }

  private void updateForSpec(String text) {
    boolean newVis = !text.equals(desc.matchSpec) && editBox.visible;
    if (newVis != saveButton.visible) {
      System.out.printf("Save button: %s\n", newVis);
    }
    saveButton.visible = newVis;
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
    boolean newVis = desc.mode == MATCHES;
    if (newVis != editBox.visible) {
      System.out.printf("Edit Box visible: %s\n", newVis);
    }
    editBox.visible = newVis;
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
    storeText();
    sendFilterDesc();
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
