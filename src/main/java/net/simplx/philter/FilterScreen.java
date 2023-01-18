package net.simplx.philter;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.Arrays.stream;
import static net.minecraft.util.math.Direction.DOWN;
import static net.simplx.mcgui.Colors.LABEL_COLOR;
import static net.simplx.mcgui.Horizontal.CENTER;
import static net.simplx.mcgui.Horizontal.LEFT;
import static net.simplx.mcgui.Horizontal.RIGHT;
import static net.simplx.mcgui.Vertical.ABOVE;
import static net.simplx.mcgui.Vertical.BELOW;
import static net.simplx.mcgui.Vertical.MID;
import static net.simplx.philter.FilterMode.MATCHES;
import static net.simplx.philter.FilterMode.NONE;
import static net.simplx.philter.FilterMode.values;
import static net.simplx.philter.PhilterMod.FILTER_ID;
import static net.simplx.philter.PhilterMod.MOD_ID;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
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
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.RotationAxis;
import net.simplx.mcgui.Layout;
import net.simplx.mcgui.Layout.Placer;
import net.simplx.mcgui.RadioButtonWidget;
import net.simplx.mcgui.RadioButtons;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("SameParameterValue")
@Environment(EnvType.CLIENT)
public class FilterScreen extends HandledScreen<FilterScreenHandler> {

  private static final Logger LOGGER = LoggerFactory.getLogger(FilterScreen.class);

  private static final Identifier TEXTURE = new Identifier(MOD_ID,
      "textures/gui/container/filter.png");
  private static final Identifier FILTER_DOWN_FACING_TOP = new Identifier(MOD_ID,
      "textures/block/filter_down_facing_top.png");
  private static final Identifier FILTER_DOWN_FILTER_TOP = new Identifier(MOD_ID,
      "textures/block/filter_down_filter_top_on.png");
  private static final Identifier FILTER_SIDE_FACING_TOP = new Identifier(MOD_ID,
      "textures/block/filter_side_facing_top.png");
  private static final Identifier FILTER_SIDE_FILTER_TOP = new Identifier(MOD_ID,
      "textures/block/filter_side_filter_top_on.png");
  public static final int TOP_SIZE = 32;
  public static final int TOP_MID = TOP_SIZE / 2;

  private static final int TITLE_TEXT_COLOR = LABEL_COLOR; // A constant in minecraft source...somewhere?

  private static final int SCREEN_H = 227;
  private static final int SCREEN_W = 346;

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
  private boolean initializing;
  private Placer titlePlace;
  private Placer topP;
  private Placer hopperArea;

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
    Function<FilterMode, Text> modeTextGen = mode -> (Text) layout.text(
        "mode." + mode.toString().toLowerCase());
    Placer modeP = p = layout.placer().withTexts(stream(values()).map(modeTextGen).toList())
        .inButton().x(RIGHT, titlePlace).y(ABOVE);
    titlePlace.y(MID, modeP);
    CyclingButtonWidget<FilterMode> modeButton = addDrawableChild(
        CyclingButtonWidget.builder(modeTextGen).values(values()).omitKeyText().initially(desc.mode)
            .tooltip(value -> layout.tooltip("mode." + value.toString().toLowerCase() + ".tooltip"))
            .build(p.x(), p.y(), p.w(), p.h(), null, (button, m) -> setMode(m)));

    Text allText = layout.text("all");
    Text anyText = layout.text("any");
    p = layout.placer().withTexts(anyText, allText).inButton().x(RIGHT, modeP).y(ABOVE);
    allButton = addDrawableChild(
        CyclingButtonWidget.onOffBuilder(anyText, allText).initially(true).omitKeyText()
            .tooltip(value -> layout.tooltip((value ? "all" : "any") + ".tooltip"))
            .build(p.x(), p.y(), p.w(), p.h(), null, (button, all) -> setAll(all)));

    Text exactText = layout.text("exact");
    Placer exactP = p = layout.placer().w(layout.onOffButtonW(exactText)).h(layout.textH).inButton()
        .x(titlePlace.x()).y(BELOW, layout.placer(modeButton));
    exactButton = addDrawableChild(CyclingButtonWidget.onOffBuilder(desc.exact)
        .tooltip(value -> layout.tooltip("exact." + value + ".tooltip"))
        .build(p.x(), p.y(), p.w(), p.h(), exactText, (button, exact) -> setExact(exact)));

    p = layout.placer().withText(saveText).inButton().x(RIGHT).y(MID, layout.placer(exactButton));
    saveButton = addDrawableChild(
        new ButtonWidget.Builder(saveText, this::save).dimensions(p.x(), p.y(), p.w(), p.h())
            .tooltip(layout.tooltip("save.tooltip")).build());

    Placer group = layout.placer().from(LEFT, titlePlace).to(RIGHT).y(BELOW, exactP);
    // inTextField adjust any known dimension for text field boundaries, but the width doesn't need
    // any text field padding, it's based on total width, so we set it after.
    Placer first = group.clone().h(layout.textH + 3).w(group.w() / 2);
    Placer bottom = first.y(BELOW);

    matchesFields = new ArrayList<>();
    boolean foundFocus = false;
    TextFieldWidget field = null;
    for (int i = 0; true; i++) {
      int col = i % 2;
      int row = i / 2;
      p = first.clone().x(group.x() + col * first.w()).y(group.y() + row * first.h());
      if (p.y() > bottom.y()) {
        break;
      }
      field = addDrawableChild(
          new TextFieldWidget(textRenderer, p.x(), p.y(), p.w(), p.h(), Text.empty()));
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

    hopperArea = layout.placer().from(LEFT).to(x + hopperWidth).from(ABOVE)
        .to(hopperHeight - layout.borderH);
    Placer mid = layout.placer().w(hopperArea.w()).from(BELOW, hopperArea).to(BELOW)
        .x(hopperArea.x()).y(BELOW, hopperArea);
    topP = layout.placer().size(TOP_SIZE, TOP_SIZE).x(CENTER, mid).y(MID, mid);

    boolean facingDown = handler.facing == DOWN;
    Direction dir = handler.userFacingDir;
    directionButtons = new RadioButtons<>();
    for (int i = 0; i < 4; i++) {
      Direction toDir = dir == handler.facing ? DOWN : dir;
      Placer q = layout.placer().inCheckbox();
      switch (i) {
        case 1 -> q.x(RIGHT, topP).y(MID, topP);
        case 2 -> q.x(CENTER, topP).y(BELOW, topP);
        case 3 -> q.x(LEFT, topP).y(MID, topP);
        case 0 -> q.x(CENTER, topP).y(ABOVE, topP);
      }
      directionButtons.add(
          addDrawableChild(new RadioButtonWidget<>(toDir, q.x(), q.y(), q.w(), q.h(), null)));
      dir = dir.rotateClockwise(Axis.Y);
    }
    directionButtons.setUpdateCallback(this::setFitlerDir);
    directionButtons.findButton(handler.filter).setChecked(true);

    initializing = false;
    reactToChange();
  }

  private Void setFitlerDir(RadioButtons<Direction> unused, Direction dir) {
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
    var wasVisible = matchesFields.get(0).visible;
    boolean newVisible = desc.mode == MATCHES;
    if (!wasVisible && newVisible) {
      setInitialFocus(matchesFields.get(0));
    } else if (wasVisible && !newVisible) {
      if (matchesFields.stream().anyMatch(ClickableWidget::isFocused)) {
        TextFieldWidget lastField = matchesFields.get(matchesFields.size() - 1);
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
    desc.matches = matchesFields.stream().map(TextFieldWidget::getText)
        .filter(text -> !text.isBlank()).collect(toImmutableList());
  }

  private void save(ButtonWidget unused) {
    storeText();
    sendFilterDesc();
    reactToChange();
  }

  private void sendFilterDesc() {
    try {
      ClientPlayNetworking.send(FILTER_ID,
          desc.packetBuf(handler.getPos(), handler.facing, handler.filter));
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

    Placer p = layout.placer().x(LEFT).y(BELOW, hopperArea).inLabel();
    textRenderer.draw(matrices, layout.text("filter_dir"), p.x(), p.y() + layout.textH, LABEL_COLOR);

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
