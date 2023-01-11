package net.simplx.philter;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.Arrays.stream;
import static net.simplx.mcgui.Horizontal.LEFT;
import static net.simplx.mcgui.Horizontal.RIGHT;
import static net.simplx.mcgui.Vertical.ABOVE;
import static net.simplx.mcgui.Vertical.BELOW;
import static net.simplx.mcgui.Vertical.MID;
import static net.simplx.philter.FilterMode.MATCHES;
import static net.simplx.philter.FilterMode.NONE;
import static net.simplx.philter.FilterMode.values;
import static net.simplx.philter.PhilterMod.MOD_ID;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
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
import net.simplx.mcgui.Layout;
import net.simplx.mcgui.Layout.Placer;
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

  private static final int MODE_X = 176;

  private static final Pattern RESOURCE_PAT = Pattern.compile("!?#[-a-z0-9_./]+");

  private final FilterDesc desc;

  private MutableText filterTitle;
  private CyclingButtonWidget<Boolean> exactButton;
  private List<TextFieldWidget> matchesFields;
  private CyclingButtonWidget<Boolean> allButton;
  private ButtonWidget saveButton;
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

  protected void init() {
    initializing = true;
    super.init();

    layout = new Layout(this);
    layout.setPrefix("philter.filter");

    // Calculate the text- and font-relative values.
    Text saveText = layout.text("save");
    filterTitle = layout.text("name").append(":");

    titlePlace = layout.placer().w(layout.textW(filterTitle)).h(layout.textH).relX(MODE_X).y(ABOVE);

    Placer p;
    Function<FilterMode, Text> modeTextGen = mode -> (Text) layout.text(
        "mode." + mode.toString().toLowerCase());
    p = layout.placer().withTexts(stream(values()).map(modeTextGen).toList()).inButton()
        .x(RIGHT, titlePlace).y(MID, titlePlace);
    var modeButton = addDrawableChild(
        CyclingButtonWidget.builder(modeTextGen).values(values()).omitKeyText()
            .initially(desc.mode)
            .tooltip(value -> layout.tooltip("mode." + value.toString().toLowerCase() + ".tooltip"))
            .build(p.x(), p.y(), p.w(), p.h(), null, (button, m) -> setMode(m)));

    Text allText = layout.text("all");
    Text anyText = layout.text("any");
    p = layout.placer().withTexts(anyText, allText).inButton().x(RIGHT).y(MID, titlePlace);
    allButton = addDrawableChild(
        CyclingButtonWidget.onOffBuilder(anyText, allText).initially(true).omitKeyText()
            .build(p.x(), p.y(), p.w(), p.h(), null, (button, all) -> setAll(all)));

    Text exactText = layout.text("exact");
    p = layout.placer().w(layout.onOffButtonW(exactText)).h(layout.textH).inButton()
        .x(titlePlace.x()).y(BELOW, modeButton);
    exactButton = addDrawableChild(CyclingButtonWidget.onOffBuilder(desc.exact)
        .tooltip(value -> layout.tooltip("exact." + value + ".tooltip"))
        .build(p.x(), p.y(), p.w(), p.h(), exactText, (button, exact) -> setExact(exact)));

    p = layout.placer().withText(saveText).inButton().x(RIGHT).y(MID, exactButton);
    saveButton = addDrawableChild(
        new ButtonWidget.Builder(saveText, this::save).dimensions(p.x(), p.y(), p.w(), p.h())
            .tooltip(layout.tooltip("tooltip")).build());

    Placer group = layout.placer().from(LEFT, titlePlace).to(RIGHT).y(BELOW, exactButton);
    // inTextField adjust any known dimension for text field boundaries, but the width doesn't need
    // any text field padding, it's based on total width, so we set it after.
    Placer first = group.clone().inTextField().w(group.w() / 2);
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
    initializing = false;
    reactToChange();
  }

  @Override
  protected void drawForeground(MatrixStack matrices, int mouseX, int mouseY) {
    super.drawForeground(matrices, mouseX, mouseY);
    layout.drawText(matrices, titlePlace, filterTitle, TITLE_TEXT_COLOR);
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
