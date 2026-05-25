package net.simplx.philter;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.simplx.mcgui.Layout;
import net.simplx.mcgui.Layout.Placer;
import net.simplx.mcgui.RadioButtonWidget;
import net.simplx.mcgui.RadioButtons;
import org.joml.Matrix3x2fStack;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.Arrays.stream;
import static net.minecraft.core.Direction.DOWN;
import static net.simplx.mcgui.Colors.LABEL_COLOR;
import static net.simplx.mcgui.Horizontal.*;
import static net.simplx.mcgui.Vertical.*;
import static net.simplx.philter.FilterMode.values;
import static net.simplx.philter.FilterMode.*;
import static net.simplx.philter.FilterScreenHandler.EXAMPLES_GRID_X;
import static net.simplx.philter.FilterScreenHandler.EXAMPLES_GRID_Y;
import static net.simplx.philter.PhilterMod.MOD_ID;

@SuppressWarnings("SameParameterValue")
@Environment(EnvType.CLIENT)
public class FilterScreen extends AbstractContainerScreen<FilterScreenHandler> {

  private static final Logger LOGGER = LoggerFactory.getLogger(FilterScreen.class);

  private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(MOD_ID,
      "textures/gui/container/filter.png");
  public static final int TEXTURE_WIDTH = 512;
  public static final int TEXTURE_HEIGHT = 256;

  private static final Identifier FILTER_DOWN_FACING_TOP = Identifier.fromNamespaceAndPath(MOD_ID,
      "textures/block/filter_down_facing_top.png");
  private static final Identifier FILTER_DOWN_FILTER_TOP = Identifier.fromNamespaceAndPath(MOD_ID,
      "textures/block/filter_down_filter_top_on.png");
  private static final Identifier FILTER_SIDE_FACING_TOP = Identifier.fromNamespaceAndPath(MOD_ID,
      "textures/block/filter_side_facing_top.png");
  private static final Identifier FILTER_SIDE_FILTER_TOP = Identifier.fromNamespaceAndPath(MOD_ID,
      "textures/block/filter_side_filter_top_on.png");
  public static final int TOP_SIZE = 32;
  public static final int TOP_MID = TOP_SIZE / 2;

  private static final int TITLE_TEXT_COLOR = LABEL_COLOR;

  private static final int SCREEN_H = 227;
  private static final int SCREEN_W = 346;
  private static final int EXAMPLES_W = 92;
  private static final int EXAMPLES_H = 92;
  private static final int EXAMPLES_BORDER = 10;

  private static final int MODE_X = 176;

  private static final Pattern RESOURCE_PAT = Pattern.compile("!?#[-a-z0-9_./]+");
  private RadioButtons<Direction> directionButtons;
  private final FilterDesc desc;
  private MutableComponent titleText;
  private CycleButton<Boolean> exactButton;
  private List<EditBox> matchesFields;
  private final int hopperWidth, hopperHeight;
  private Layout layout;
  private CycleButton<Boolean> allButton;
  private Button saveButton;
  private MultiLineTextWidget exampleText;
  private boolean initializing;
  private Placer titlePlace;
  private Placer topP;
  private Placer examplesP;
  private Placer exampleBgP;
  private Placer dirP;

  public FilterScreen(FilterScreenHandler handler, Inventory inventory, Component title) {
    super(handler, inventory, title, SCREEN_W, SCREEN_H);
    hopperWidth = 176;
    hopperHeight = 133;
    inventoryLabelY = hopperHeight - 94;
    desc = handler.getFilterDesc();
  }

  @Override
  protected void init() {
    initializing = true;
    super.init();

    layout = new Layout(this);
    layout.setPrefix("philter.filter");

    Component saveText = layout.text("save");
    titleText = layout.text("name").append(":");
    titlePlace = layout.placer().withText(titleText).x(leftPos + MODE_X).y(ABOVE);

    Placer p;
    Function<FilterMode, Component> modeTextGen = mode -> layout.text("mode." + mode.toString().toLowerCase());
    Placer modeP = p = layout.placer().withTexts(stream(values()).map(modeTextGen).toList()).inButton().x(RIGHT,
        titlePlace).y(ABOVE);
    titlePlace.y(MID, modeP);
    CycleButton<FilterMode> modeButton =
        addRenderableWidget(CycleButton.builder(modeTextGen, desc.mode).withValues(values()).displayOnlyValue().withTooltip(
            value -> layout.tooltip("mode." + value.toString().toLowerCase() + ".tooltip")).create(p.x(), p.y(), p.w(),
            p.h(), Component.empty(), (button, m) -> setMode(m)));

    Component allText = layout.text("all");
    Component anyText = layout.text("any");
    p = layout.placer().withTexts(anyText, allText).inButton().x(RIGHT, modeP).y(ABOVE);
    allButton =
        addRenderableWidget(CycleButton.booleanBuilder(anyText, allText, true).displayOnlyValue().withTooltip(
            value -> layout.tooltip((value ? "all" : "any") + ".tooltip")).create(p.x(), p.y(), p.w(), p.h(),
            Component.empty(), (button, all) -> setAll(all)));

    Component exactText = layout.text("exact");
    Placer exactP = p =
        layout.placer().w(layout.onOffButtonW(exactText)).h(layout.textH).inButton().x(titlePlace.x()).y(BELOW,
            layout.placer(modeButton));
    exactButton = addRenderableWidget(CycleButton.onOffBuilder(desc.exact).withTooltip(value -> layout.tooltip(
        "exact." + value + ".tooltip")).create(p.x(), p.y(), p.w(), p.h(), exactText,
        (button, exact) -> setExact(exact)));

    p = layout.placer().withText(saveText).inButton().x(RIGHT).y(MID, layout.placer(exactButton));
    saveButton = addRenderableWidget(Button.builder(saveText, this::save).bounds(p.x(), p.y(), p.w(), p.h()).tooltip(
        layout.tooltip("save.tooltip")).build());

    Placer changing = layout.placer().from(LEFT, titlePlace).to(RIGHT).from(BELOW, exactP).to(BELOW);

    Placer first = changing.clone().h(layout.textH + 3).w(changing.w() / 2);
    Placer bottom = first.y(BELOW);

    matchesFields = new ArrayList<>();
    boolean foundFocus = false;
    EditBox field = null;
    for (int i = 0; true; i++) {
      int col = i % 2;
      int row = i / 2;
      p = first.clone().x(changing.x() + col * first.w()).y(changing.y() + row * (first.h() + 2));
      if (p.y() > bottom.y()) {
        break;
      }
      field = addRenderableWidget(new EditBox(font, p.x(), p.y(), p.w(), p.h(), Component.empty()));
      matchesFields.add(field);
      final int index = i;
      field.setEditable(true);
      field.setResponder(text -> matchChanged(index, text));
      String match = desc.match(i);
      field.setValue(match);
      if (!foundFocus && match.isEmpty()) {
        field.setFocused(true);
        this.setFocused(field);
        foundFocus = true;
        setInitialFocus(field);
      }
      field.visible = false;
    }
    if (!foundFocus) {
      assert field != null;
      setInitialFocus(field);
    }
    setMatchesVisible(false);

    Placer hopperP = layout.placer().from(LEFT).to(leftPos + hopperWidth).from(ABOVE).to(hopperHeight - layout.borderH);
    Placer mid = layout.placer().w(hopperP.w()).from(BELOW, hopperP).to(BELOW).x(hopperP.x()).y(BELOW, hopperP);
    topP = layout.placer().size(TOP_SIZE, TOP_SIZE).x(CENTER, mid).y(BOTTOM);
    topP.y(topP.y() - layout.placer().inCheckbox().h() - layout.gapH);
    examplesP = layout.placer().size(EXAMPLES_W, EXAMPLES_H).x(CENTER, changing).y(TOP_EDGE, changing);
    examplesP.y(examplesP.y() - 12);
    int examplesX = examplesP.x() - leftPos + EXAMPLES_BORDER + 1;
    int examplesY = examplesP.y() - topPos + EXAMPLES_BORDER + 1;
    if (examplesX != EXAMPLES_GRID_X || examplesY != EXAMPLES_GRID_Y) {
      LOGGER.warn(String.format("Filter misalignment: expected at (%d, %d), not (%d, %d)", EXAMPLES_GRID_X,
          EXAMPLES_GRID_Y, examplesX, examplesY));
    }
    int width1 = changing.w() * 3 / 4;
    MultiLineTextWidget exampleTextWidget = new MultiLineTextWidget(layout.text("examples"), font);
    exampleTextWidget.setMaxWidth(width1);
    exampleTextWidget.setCentered(true);
    p = layout.placer().w(width1).x(CENTER, examplesP).y(BELOW, examplesP);
    this.exampleText = addRenderableWidget(p.place(exampleTextWidget));
    int bgBorder = 3;
    exampleBgP = layout.placer().size(this.exampleText.getWidth() + 2 * bgBorder,
        this.exampleText.getHeight() + 2 * bgBorder).at(this.exampleText.getX() - bgBorder,
        this.exampleText.getY() - bgBorder);

    Direction dir = menu.userFacingDir;
    directionButtons = new RadioButtons<>();
    dirP = layout.placer().withText("filter_dir").x(LEFT);
    for (int i = 0; i < 4; i++) {
      Direction toDir = dir == menu.facing ? DOWN : dir;
      Placer q = layout.placer().inCheckbox();
      switch (i) {
        case 1 -> q.x(RIGHT, topP).y(MID, topP);
        case 2 -> q.x(CENTER, topP).y(BELOW, topP);
        case 3 -> q.x(LEFT, topP).y(MID, topP);
        case 0 -> q.x(CENTER, topP).y(ABOVE, topP);
      }
      directionButtons.add(addRenderableWidget(new RadioButtonWidget<>(toDir, q.x(), q.y(), 100,
          Component.empty())));
      dir = dir.getClockWise(Axis.Y);
      if (i == 0) {
        dirP.y(q.y() + layout.gapH);
      }
    }
    directionButtons.setUpdateCallback(this::setFilterDir);
    directionButtons.findButton(menu.filter).setChecked(true);

    initializing = false;
    adjustToMode();
  }

  @Override
  public boolean isPauseScreen() {
    return false;
  }

  // In some modern Mojmap variants, this forces the window to grab focus:
  public boolean shouldCloseOnEsc() {
    return true;
  }


  private Void setFilterDir(RadioButtons<Direction> unused, Direction dir) {
    menu.filter = dir;
    return null;
  }

  @Override
  public void extractBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float delta) {
    super.extractBackground(extractor, mouseX, mouseY, delta);
    extractor.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0f, 0f,
        imageWidth, imageHeight, TEXTURE_WIDTH, TEXTURE_HEIGHT);

    if (desc.mode == SAME_AS) {
      Placer p = examplesP;
      extractor.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, p.x(), p.y(), (float) SCREEN_W, 0f,
          p.w(), p.h(), TEXTURE_WIDTH, TEXTURE_HEIGHT);
      p = exampleBgP;
      extractor.fill(p.x(), p.y(), p.endX(), p.endY(), 0xff404040);
    }
  }

  @Override
  protected void extractLabels(GuiGraphicsExtractor extractor, int mouseX, int mouseY) {
    super.extractLabels(extractor, mouseX, mouseY);
    layout.drawText(extractor, font, titlePlace, titleText, TITLE_TEXT_COLOR);
    extractor.text(font, layout.text("filter_dir"), dirP.x(), dirP.y(), LABEL_COLOR, false);
  }

  private void matchChanged(int i, String text) {
    String spec = text.trim();
    EditBox field = matchesFields.get(i);
    if (!spec.equals(text)) {
      field.setValue(spec);
      return;
    }

    field.setTooltip(null);
    if (!spec.isEmpty() && !RESOURCE_PAT.matcher(spec).matches()) {
      try {
        Pattern.compile(spec, Pattern.CASE_INSENSITIVE);
      } catch (PatternSyntaxException e) {
        field.setTooltip(Tooltip.create(Component.literal(e.getMessage())));
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
    Arrays.stream(menu.filterSlots).forEach(slot -> slot.setEnabled(desc.mode == SAME_AS));

    boolean wasVisible = matchesFields.get(0).visible;
    boolean newVisible = desc.mode == MATCHES;
    setMatchesVisible(newVisible);
    if (!wasVisible && newVisible) {
      setInitialFocus(matchesFields.get(0));
    } else if (wasVisible && !newVisible) {
      if (matchesFields.stream().anyMatch(AbstractWidget::isFocused)) {
        EditBox lastField = matchesFields.get(matchesFields.size() - 1);
        setInitialFocus(lastField);
        lastField.setFocused(true);
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
      if (!matchesFields.get(i).getValue().equals(orig)) {
        return true;
      }
    }
    return false;
  }

  private void setMatchesVisible(boolean newVisible) {
    matchesFields.forEach(field -> field.visible = newVisible);
  }

  private void storeText() {
    desc.matches =
        matchesFields.stream().map(EditBox::getValue).filter(text -> !text.isBlank()).collect(toImmutableList());
  }

  private void save(Button unused) {
    storeText();
    sendFilterDesc();
    adjustToMode();
  }

  private void sendFilterDesc() {
    try {
      ClientPlayNetworking.send(new FilterData(menu.getFilterDesc(), menu.getPos(), menu.facing,
          menu.filter, null));
    } catch (NullPointerException e) {
      LOGGER.error("Unexpected null", e);
    }
  }

  @Override
  public void extractContents(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float delta) {
    super.extractContents(extractor, mouseX, mouseY, delta);

    drawTop(extractor, menu.facing, FILTER_DOWN_FACING_TOP, FILTER_SIDE_FACING_TOP);
    RadioButtonWidget<Direction> button = directionButtons.getOn();
    drawTop(extractor, button.getValue(), FILTER_DOWN_FILTER_TOP, FILTER_SIDE_FILTER_TOP);
  }

  void drawTop(GuiGraphicsExtractor extractor, Direction dir, Identifier down, Identifier side) {
    Matrix3x2fStack matrices = extractor.pose();
    matrices.pushMatrix();
    try {
      matrices.translate(topP.x() + TOP_MID, topP.y() + TOP_MID);
      Identifier texture = down;
      if (dir != DOWN) {
        int rot = (dir.get2DDataValue() - menu.userFacingDir.get2DDataValue()) * 90;
        matrices.rotate((float) Math.toRadians(rot));
        texture = side;
      }
      matrices.translate(-TOP_MID, -TOP_MID);
      extractor.blit(RenderPipelines.GUI_TEXTURED, texture, 0, 0, 0f, 0f, topP.w(), topP.h(), topP.w(), topP.h());
    } finally {
      matrices.popMatrix();
    }
  }

  @SuppressWarnings("unused")
  private void drawBox(GuiGraphicsExtractor extractor, Placer p, int color) {
    if (p != null) {
      drawBox(extractor, p.x(), p.y(), p.w(), p.h(), color);
    }
  }

  private void drawBox(GuiGraphicsExtractor extractor, int x, int y, int width, int height, int color) {
    extractor.horizontalLine(x, x + width, y, color);
    extractor.horizontalLine(x, x + width, y + height, color);
    extractor.verticalLine(x, y, y + height, color);
    extractor.verticalLine(x + width, y, y + height, color);
  }

  @Override
  public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean isDrag) {
    boolean result = super.mouseClicked(event, isDrag);
    // After any click, ContainerEventHandler.mouseClicked calls setFocused(clickedWidget)
    // which overwrites any setInitialFocus we did in adjustToMode. Reclaim focus.
    if (desc.mode == MATCHES && !(getFocused() instanceof EditBox)) {
      for (EditBox field : matchesFields) {
        if (field.isActive()) {
          setFocused(field);
          break;
        }
      }
    }
    return result;
  }

  @Override
  public boolean keyPressed(KeyEvent event) {
    GuiEventListener focused = getFocused();
    if (focused instanceof EditBox editBox && editBox.isActive()) {
      // Prevent 'E' from closing the screen
      if (minecraft.options.keyInventory.matches(event)) {
        return true;
      }

      // Check for GLFW Escape keycode (usually 256)
      if (event.key() == 256) {
        if (minecraft.player != null)
          minecraft.player.closeContainer();
        return true;
      }
      return editBox.keyPressed(event);
    }
    return super.keyPressed(event);
  }

  @Override
  public boolean charTyped(CharacterEvent event) {
    if (getFocused() instanceof EditBox editBox && editBox.isActive()) {
      // Let the EditBox handle the event natively!
      // This validates the character, moves the cursor, and redraws the text.
      editBox.setFocused(true);
      return editBox.charTyped(event);
    }
    return super.charTyped(event);
  }

  @Override
  public void onClose() {
    if (anyMatchChanged()) {
      storeText();
    }
    sendFilterDesc();
    super.onClose();
  }
}
