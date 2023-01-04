package net.simplx.philter;

import io.github.cottonmc.cotton.gui.SyncedGuiDescription;
import io.github.cottonmc.cotton.gui.widget.WBox;
import io.github.cottonmc.cotton.gui.widget.WGridPanel;
import io.github.cottonmc.cotton.gui.widget.WItemSlot;
import io.github.cottonmc.cotton.gui.widget.WLabel;
import io.github.cottonmc.cotton.gui.widget.WListPanel;
import io.github.cottonmc.cotton.gui.widget.WScrollPanel;
import io.github.cottonmc.cotton.gui.widget.WTabPanel;
import io.github.cottonmc.cotton.gui.widget.WTabPanel.Tab;
import io.github.cottonmc.cotton.gui.widget.data.Axis;
import io.github.cottonmc.cotton.gui.widget.data.Insets;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class FilterGuiDescription extends SyncedGuiDescription {

  private static final Text NO_NEW_TEXT = Text.translatable("philter.filter.mode.no_new");
  private static final Text MATCHES_TEXT = Text.translatable("philter.filter.mode.matches");

  public FilterGuiDescription(int syncId, PlayerInventory inventory, ScreenHandlerContext context) {
    super(PhilterMod.FILTER_SCREEN_HANDLER_TYPE, syncId, inventory,
        getBlockInventory(context, HopperBlockEntity.INVENTORY_SIZE),
        getBlockPropertyDelegate(context));
    WBox root = new WBox(Axis.HORIZONTAL);
    setRootPanel(root);

    WBox hopper = new WBox(Axis.VERTICAL);
    root.add(hopper);
    hopper.setInsets(new Insets(20, 0, 0, 0));
    // hopper.setInsets(Insets.ROOT_PANEL);

    WGridPanel blockInv = new WGridPanel();
    hopper.add(blockInv);
    for (int i = 0; i < HopperBlockEntity.INVENTORY_SIZE; i++) {
      WItemSlot itemSlot = WItemSlot.of(blockInventory, i);
      blockInv.add(itemSlot, 2 + i, 0);
    }
    hopper.add(createPlayerInventoryPanel());

    WTabPanel filter = new WTabPanel();
    root.add(filter);

    Tab noNewMode = new WTabPanel.Tab.Builder(new WLabel(NO_NEW_TEXT)).title(NO_NEW_TEXT).build();
    filter.add(noNewMode);

    // the panel that holds all labels for each match spec
    WBox matchSpecs = new WBox(Axis.VERTICAL);
    root.add(matchSpecs);
    BiConsumer<String, WLabel> matchSpecCreator = (String str, WLabel label) -> {
      label.setText(Text.literal(str));
    };

    Supplier<WLabel> labelSupplier = () -> new WLabel(Text.literal("foo"));
    WListPanel<String, WLabel> matchesList = new WListPanel<>(List.of("one", "two", "three"),
        labelSupplier, matchSpecCreator);

    WScrollPanel scroller = new WScrollPanel(matchesList);
    Tab matchesMode = new WTabPanel.Tab.Builder(scroller).title(MATCHES_TEXT)
        .build();
    filter.add(matchesMode);

    root.validate(this);
  }

  public FilterGuiDescription(ScreenHandlerType<?> type, int syncId,
      PlayerInventory playerInventory, @Nullable Inventory blockInventory,
      @Nullable PropertyDelegate propertyDelegate) {
    super(type, syncId, playerInventory, blockInventory, propertyDelegate);
  }
}
