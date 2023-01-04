package net.simplx.philter;

import io.github.cottonmc.cotton.gui.SyncedGuiDescription;
import io.github.cottonmc.cotton.gui.widget.WBox;
import io.github.cottonmc.cotton.gui.widget.WGridPanel;
import io.github.cottonmc.cotton.gui.widget.WItemSlot;
import io.github.cottonmc.cotton.gui.widget.WLabel;
import io.github.cottonmc.cotton.gui.widget.data.Axis;
import io.github.cottonmc.cotton.gui.widget.data.Insets;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

public class FilterGuiDescription extends SyncedGuiDescription {

  public FilterGuiDescription(int syncId, PlayerInventory inventory, ScreenHandlerContext context) {
    super(PhilterMod.FILTER_SCREEN_HANDLER_TYPE, syncId, inventory,
        getBlockInventory(context, HopperBlockEntity.INVENTORY_SIZE),
        getBlockPropertyDelegate(context));
    WBox root = new WBox(Axis.VERTICAL);
    setRootPanel(root);
    root.setInsets(new Insets(20, 0, 0, 0));
    root.setSize(133, 200);
    root.setInsets(Insets.ROOT_PANEL);

    WGridPanel blockInv = new WGridPanel();
    for (int i = 0; i < HopperBlockEntity.INVENTORY_SIZE; i++) {
      WItemSlot itemSlot = WItemSlot.of(blockInventory, i);
      blockInv.add(itemSlot, 2 + i, 0);
    }
    root.add(blockInv);
    root.add(createPlayerInventoryPanel());
    root.validate(this);
  }

  public FilterGuiDescription(ScreenHandlerType<?> type, int syncId,
      PlayerInventory playerInventory, @Nullable Inventory blockInventory,
      @Nullable PropertyDelegate propertyDelegate) {
    super(type, syncId, playerInventory, blockInventory, propertyDelegate);
  }
}
