package net.simplx.philter;

import static net.simplx.philter.PhilterMod.MOD_ID;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Collections;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class FilterScreen extends HandledScreen<FilterScreenHandler> {

  private static final Identifier TEXTURE = new Identifier(MOD_ID,
      "textures/gui/container/filter.png");

  private static final Text FILTER_TITLE = Text.translatable("philter.filter.name").append(":");
  public static final int TITLE_TEXT_COLOR = 4210752; // A constant in minecraft source...somewhere?
  private static final int TEXT_HEIGHT = MinecraftClient.getInstance().textRenderer.fontHeight + 2;
  private static final int FILTER_Y = 130 + TEXT_HEIGHT / 2;
  private static final int BORDER = 8;
  private static final int BUTTON_HEIGHT = TEXT_HEIGHT + 2;
  public static final int BUTTON_WIDTH;

  private final FilterDesc filterDesc;

  static {
    var renderer = MinecraftClient.getInstance().textRenderer;
    var maxWidth = 0;
    for (FilterMode mode : FilterMode.values()) {
      maxWidth = Math.max(maxWidth, renderer.getWidth(mode.asString()));
    }
    // 2: Space on either side
    BUTTON_WIDTH = maxWidth + renderer.getWidth(" ") * 2;
  }

  public FilterScreen(FilterScreenHandler handler, PlayerInventory inventory, Text title) {
    super(handler, inventory, title);
    passEvents = false;
    backgroundHeight = 233;
    playerInventoryTitleY = backgroundHeight - 194;
    filterDesc = handler.getFilterDesc();
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
            .omitKeyText()
            .initially(filterDesc.mode)
            .build(x + BORDER + textWidth, FILTER_Y, BUTTON_WIDTH, BUTTON_HEIGHT, null,
                (button, mode) -> setMode(mode)));
  }

  private static Text filterText(FilterMode value) {
    return Text.translatable("philter.filter.mode." + value.asString());
  }

  private void setMode(FilterMode mode) {
    filterDesc.mode = mode;
    client.player.networkHandler.sendPacket(new FilterPacket(filterDesc, handler.getPos()));
  }

  public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
    renderBackground(matrices);
    super.render(matrices, mouseX, mouseY, delta);
    drawMouseoverTooltip(matrices, mouseX, mouseY);
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
