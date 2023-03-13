package net.simplx.philter;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.*;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.RotationAxis;
import net.simplx.mcgui.Layout;
import net.simplx.mcgui.Layout.Placer;
import net.simplx.mcgui.RadioButtonWidget;
import net.simplx.mcgui.RadioButtons;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.Arrays.stream;
import static net.minecraft.util.math.Direction.DOWN;
import static net.simplx.mcgui.Colors.LABEL_COLOR;
import static net.simplx.mcgui.Horizontal.*;
import static net.simplx.mcgui.Vertical.*;
import static net.simplx.philter.FilterMode.values;
import static net.simplx.philter.FilterMode.*;
import static net.simplx.philter.FilterScreenHandler.EXAMPLES_GRID_X;
import static net.simplx.philter.FilterScreenHandler.EXAMPLES_GRID_Y;
import static net.simplx.philter.PhilterMod.FILTER_ID;
import static net.simplx.philter.PhilterMod.MOD_ID;

@SuppressWarnings("SameParameterValue")
@Environment(EnvType.CLIENT)
public class FilterScreen extends HandledScreen<FilterScreenHandler> {

  private static final Logger LOGGER = LoggerFactory.getLogger(FilterScreen.class);

  private static final Identifier TEXTURE = new Identifier(MOD_ID, "textures/gui/container/filter.png");
  public static final int TEXTURE_WIDTH = 512;
  public static final int TEXTURE_HEIGHT = 256;

  private static final Identifier FILTER_DOWN_FACING_TOP = new Identifier(MOD_ID, "textures/block/filter_down_facing_top.png");
  private static final Identifier FILTER_DOWN_FILTER_TOP = new Identifier(MOD_ID, "textures/block/filter_down_filter_top_on.png");
  private static final Identifier FILTER_SIDE_FACING_TOP = new Identifier(MOD_ID, "textures/block/filter_side_facing_top.png");
  private static final Identifier FILTER_SIDE_FILTER_TOP = new Identifier(MOD_ID, "textures/block/filter_side_filter_top_on.png");
  public static final int TOP_SIZE = 32;
  public static final int TOP_MID = TOP_SIZE / 2;

  private static final int TITLE_TEXT_COLOR = LABEL_COLOR; // A constant in minecraft source...somewhere?

  private static final int SCREEN_H = 227;
  private static final int SCREEN_W = 346;
  private static final int EXAMPLES_W = 92;
  private static final int EXAMPLES_H = 92;
  /**
   * Border in texture around the actual slot images (allowing for texture creators to play).
   */
  private static final int EXAMPLES_BORDER = 10;

  private static final int MODE_X = 176;

  private static final Pattern RESOURCE_PAT = Pattern.compile("!?#[-a-z0-9_./]+");
  private RadioButtons<Direction> directionButtons;
  private final FilterDesc desc;
  private MutableText titleText;
  private CyclingButtonWidget<Boolean> exactButton;
  private List<TextFieldWidget> matchesFields;
  private final int hopperWidth, hopperHeight;
  private Layout layout;
  private CyclingButtonWidget<Boolean> allButton;
  private ButtonWidget saveButton;
  private MultilineTextWidget exampleText;
  private boolean initializing;
  private Placer titlePlace;
  private Placer topP;
  private Placer hopperP;
  private Placer examplesP;
  private Placer exampleBgP;
  private Placer dirP;

  public FilterScreen(FilterScreenHandler handler, PlayerInventory inventory, Text title) {
    super(handler, inventory, title);
    backgroundWidth = SCREEN_W;
    backgroundHeight = SCREEN_H;
    passEvents = false;
    hopperWidth = 176;
    hopperHeight = 133;
    playerInventoryTitleY = hopperHeight - 94;
    desc = handler.getFilterDesc();
  }

  protected void init() {
    initializing = true;
    super.init();

    layout = new Layout(this);
    layout.setPrefix("philter.filter");

    // Calculate the text- and font-relative values.
    Text saveText = layout.text("save");
    titleText = layout.text("name").append(":");
    titlePlace = layout.placer().withText(titleText).x(x + MODE_X).y(ABOVE);

    Placer p;
    Function<FilterMode, Text> modeTextGen = mode -> (Text) layout.text("mode." + mode.toString().toLowerCase());
    Placer modeP = p = layout.placer().withTexts(stream(values()).map(modeTextGen).toList()).inButton().x(RIGHT, titlePlace).y(ABOVE);
    titlePlace.y(MID, modeP);
    CyclingButtonWidget<FilterMode> modeButton = addDrawableChild(CyclingButtonWidget.builder(modeTextGen).values(values()).omitKeyText().initially(desc.mode).tooltip(value -> layout.tooltip("mode." + value.toString().toLowerCase() + ".tooltip")).build(p.x(), p.y(), p.w(), p.h(), null, (button, m) -> setMode(m)));

    Text allText = layout.text("all");
    Text anyText = layout.text("any");
    p = layout.placer().withTexts(anyText, allText).inButton().x(RIGHT, modeP).y(ABOVE);
    allButton = addDrawableChild(CyclingButtonWidget.onOffBuilder(anyText, allText).initially(true).omitKeyText().tooltip(value -> layout.tooltip((value ? "all" : "any") + ".tooltip")).build(p.x(), p.y(), p.w(), p.h(), null, (button, all) -> setAll(all)));

    Text exactText = layout.text("exact");
    Placer exactP = p = layout.placer().w(layout.onOffButtonW(exactText)).h(layout.textH).inButton().x(titlePlace.x()).y(BELOW, layout.placer(modeButton));
    exactButton = addDrawableChild(CyclingButtonWidget.onOffBuilder(desc.exact).tooltip(value -> layout.tooltip("exact." + value + ".tooltip")).build(p.x(), p.y(), p.w(), p.h(), exactText, (button, exact) -> setExact(exact)));

    p = layout.placer().withText(saveText).inButton().x(RIGHT).y(MID, layout.placer(exactButton));
    saveButton = addDrawableChild(new ButtonWidget.Builder(saveText, this::save).dimensions(p.x(), p.y(), p.w(), p.h()).tooltip(layout.tooltip("save.tooltip")).build());

    Placer changing = layout.placer().from(LEFT, titlePlace).to(RIGHT).from(BELOW, exactP).to(BELOW);

    // inTextField adjust any known dimension for text field boundaries, but the width doesn't need
    // any text field padding, it's based on total width, so we set it after.
    Placer first = changing.clone().h(layout.textH + 3).w(changing.w() / 2);
    Placer bottom = first.y(BELOW);

    matchesFields = new ArrayList<>();
    boolean foundFocus = false;
    TextFieldWidget field = null;
    for (int i = 0; true; i++) {
      int col = i % 2;
      int row = i / 2;
      p = first.clone().x(changing.x() + col * first.w()).y(changing.y() + row * (first.h() + 2));
      if (p.y() > bottom.y()) {
        break;
      }
      field = addDrawableChild(new TextFieldWidget(textRenderer, p.x(), p.y(), p.w(), p.h(), Text.empty()));
      matchesFields.add(field);
      final int index = i;
      field.setChangedListener(text -> matchChanged(index, text));
      // Set here instead of on creation, so all text is handled through the same change mechanism.
      String match = desc.match(i);
      field.setText(match);
      if (!foundFocus && match.isEmpty()) {
        foundFocus = true;
        setInitialFocus(field);
      }
      field.visible = false;
    }
    // If we haven't found an empty field, put the focus on the last field.
    if (!foundFocus) {
      setInitialFocus(field);
    }
    setMatchesVisible(false); // ... so if it needs to be visible, it will be newly visible.

    hopperP = layout.placer().from(LEFT).to(x + hopperWidth).from(ABOVE).to(hopperHeight - layout.borderH);
    Placer mid = layout.placer().w(hopperP.w()).from(BELOW, hopperP).to(BELOW).x(hopperP.x()).y(BELOW, hopperP);
    topP = layout.placer().size(TOP_SIZE, TOP_SIZE).x(CENTER, mid).y(BOTTOM);
    topP.y(topP.y() - layout.placer().inCheckbox().h() - layout.gapH);
    examplesP = layout.placer().size(EXAMPLES_W, EXAMPLES_H).x(CENTER, changing).y(TOP_EDGE, changing);
    examplesP.y(examplesP.y() - 12); // line it up with the top of the player's inventory
    int examplesX = examplesP.x() - x + EXAMPLES_BORDER + 1;
    int examplesY = examplesP.y() - y + EXAMPLES_BORDER + 1;
    if (examplesX != EXAMPLES_GRID_X || examplesY != EXAMPLES_GRID_Y) {
      LOGGER.warn(String.format("Filter misalignment: expected at (%d, %d), not (%d, %d)", EXAMPLES_GRID_X, EXAMPLES_GRID_Y, examplesX, examplesY));
    }
    p = layout.placer().w(changing.w() * 3 / 4).x(CENTER, examplesP).y(BELOW, examplesP);
    int width1 = changing.w() * 3 / 4;
    exampleText = addDrawableChild(p.place(MultilineTextWidget.createCentered(width1, textRenderer, layout.text("examples"))));
    int bgBorder = 3;
    exampleBgP = layout.placer().size(exampleText.getWidth() + 2 * bgBorder, exampleText.getHeight() + 2 * bgBorder).at(exampleText.getX() - bgBorder, exampleText.getY() - bgBorder);

    boolean facingDown = handler.facing == DOWN;
    Direction dir = handler.userFacingDir;
    directionButtons = new RadioButtons<>();
    dirP = layout.placer().withText("filter_dir").x(LEFT);
    for (int i = 0; i < 4; i++) {
      Direction toDir = dir == handler.facing ? DOWN : dir;
      Placer q = layout.placer().inCheckbox();
      switch (i) {
        case 1 -> q.x(RIGHT, topP).y(MID, topP);
        case 2 -> q.x(CENTER, topP).y(BELOW, topP);
        case 3 -> q.x(LEFT, topP).y(MID, topP);
        case 0 -> q.x(CENTER, topP).y(ABOVE, topP);
      }
      directionButtons.add(addDrawableChild(new RadioButtonWidget<>(toDir, q.x(), q.y(), q.w(), q.h(), null)));
      dir = dir.rotateClockwise(Axis.Y);
      if (i == 0) {
        dirP.y(q.y() + layout.gapH);
      }
    }
    directionButtons.setUpdateCallback(this::setFilterDir);
    directionButtons.findButton(handler.filter).setChecked(true);

    initializing = false;
    adjustToMode();
  }

  private Void setFilterDir(RadioButtons<Direction> unused, Direction dir) {
    handler.filter = dir;
    return null;
  }

  @Override
  protected void drawForeground(MatrixStack matrices, int mouseX, int mouseY) {
    super.drawForeground(matrices, mouseX, mouseY);
    layout.drawText(matrices, titlePlace, titleText, TITLE_TEXT_COLOR);
  }

  private void matchChanged(int i, String text) {
    String spec = text.trim();
    TextFieldWidget field = matchesFields.get(i);
    if (!spec.equals(text)) {
      // This will cause recursion back into here, so just return.
      field.setText(spec);
      return;
    }

    // Color the text based on validity.
    field.setEditableColor(0xffffff);
    field.setTooltip(null);
    if (!spec.isEmpty() && !RESOURCE_PAT.matcher(spec).matches()) {
      try {
        Pattern.compile(spec, Pattern.CASE_INSENSITIVE);
      } catch (PatternSyntaxException e) {
        field.setTooltip(Tooltip.of(Text.literal(e.getMessage())));
        field.setEditableColor(0xff0000);
      }
    }
    adjustToMode();
  }

  private void setMode(FilterMode mode) {
    desc.mode = mode;
    adjustToMode();
  }

  private void setExact(boolean exact) {
    desc.exact = exact;
    sendFilterDesc();
  }

  private void setAll(Boolean all) {
    desc.matchAll = all;
    sendFilterDesc();
  }

  private void adjustToMode() {
    if (initializing) {
      return;
    }
    exactButton.visible = desc.mode != NONE;
    allButton.visible = desc.mode == MATCHES;
    saveButton.visible = allButton.visible && anyMatchChanged();
    exampleText.visible = desc.mode == SAME_AS;
    Arrays.stream(handler.filterSlots).forEach(slot -> slot.setEnabled(desc.mode == SAME_AS));

    boolean wasVisible = matchesFields.get(0).visible;
    boolean newVisible = desc.mode == MATCHES;
    setMatchesVisible(newVisible);
    if (!wasVisible && newVisible) {
      setInitialFocus(matchesFields.get(0));
    } else if (wasVisible && !newVisible) {
      if (matchesFields.stream().anyMatch(ClickableWidget::isFocused)) {
        TextFieldWidget lastField = matchesFields.get(matchesFields.size() - 1);
        setInitialFocus(lastField);
        lastField.changeFocus(true);
      }
    }

    sendFilterDesc();
  }

  private boolean anyMatchChanged() {
    if (matchesFields == null) {
      return false;
    }
    for (int i = 0; i < matchesFields.size(); i++) {
      String orig = desc.match(i);
      if (!matchesFields.get(i).getText().equals(orig)) {
        return true;
      }
    }
    return false;
  }

  private void setMatchesVisible(boolean newVisible) {
    matchesFields.forEach(field -> field.visible = newVisible);
  }

  private void storeText() {
    desc.matches = matchesFields.stream().map(TextFieldWidget::getText).filter(text -> !text.isBlank()).collect(toImmutableList());
  }

  private void save(ButtonWidget unused) {
    storeText();
    sendFilterDesc();
    adjustToMode();
  }

  private void sendFilterDesc() {
    try {
      ClientPlayNetworking.send(FILTER_ID, desc.packetBuf(handler.getPos(), handler.facing, handler.filter));
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
    drawTexture(matrices, midX, midY, 0, 0, backgroundWidth, backgroundHeight, TEXTURE_WIDTH, TEXTURE_HEIGHT);

    if (desc.mode == SAME_AS) {
      Placer p = examplesP;
      drawTexture(matrices, p.x(), p.y(), SCREEN_W, 0, p.w(), p.h(), TEXTURE_WIDTH, TEXTURE_HEIGHT);
      p = exampleBgP;
      DrawableHelper.fill(matrices, p.x(), p.y(), p.endX(), p.endY(), 0xff404040);
    }

    Placer p = layout.placer().x(LEFT).y(BELOW, hopperP).inLabel();
    textRenderer.draw(matrices, layout.text("filter_dir"), dirP.x(), dirP.y(), LABEL_COLOR);

    drawTop(matrices, handler.facing, FILTER_DOWN_FACING_TOP, FILTER_SIDE_FACING_TOP);
    RadioButtonWidget<Direction> button = directionButtons.getOn();
    drawTop(matrices, button.getValue(), FILTER_DOWN_FILTER_TOP, FILTER_SIDE_FILTER_TOP);
  }

  void drawTop(MatrixStack matrices, Direction dir, Identifier down, Identifier side) {
    matrices.push();
    matrices.translate(topP.x() + TOP_MID, topP.y() + TOP_MID, 0);
    if (dir == DOWN) {
      RenderSystem.setShaderTexture(0, down);
    } else {
      int rot = (dir.getHorizontal() - handler.userFacingDir.getHorizontal()) * 90;
      matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rot));
      RenderSystem.setShaderTexture(0, side);
    }
    matrices.translate(-TOP_MID, -TOP_MID, 0);
    drawTexture(matrices, 0, 0, 0, 0, topP.w(), topP.h(), topP.w(), topP.h());
    matrices.pop();
  }

  @SuppressWarnings("unused")
  private void drawBox(MatrixStack matrices, Placer p, int color) {
    if (p != null) {
      drawBox(matrices, p.x(), p.y(), p.w(), p.h(), color);
    }
  }

  private void drawBox(MatrixStack matrices, int x, int y, int width, int height, int color) {
    drawHorizontalLine(matrices, x, x + width, y, color);
    drawHorizontalLine(matrices, x, x + width, y + height, color);
    drawVerticalLine(matrices, x, y, y + height, color);
    drawVerticalLine(matrices, x + width, y, y + height, color);
  }
}
