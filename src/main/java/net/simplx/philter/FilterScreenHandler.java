package net.simplx.philter;

import static net.minecraft.block.entity.HopperBlockEntity.INVENTORY_SIZE;
import static net.simplx.philter.PhilterMod.FILTER_SCREEN_HANDLER;

import java.lang.reflect.Field;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.screen.HopperScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class FilterScreenHandler extends HopperScreenHandler implements Forcer {
  public static final Field TYPE_F = Forcer.field(ScreenHandler.class, "type");

  public FilterScreenHandler(int syncId, PlayerInventory playerInventory) {
    this(syncId, playerInventory, new SimpleInventory(INVENTORY_SIZE));
  }

  public FilterScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory) {
    super(syncId, playerInventory, inventory);
    forceSet(TYPE_F, FILTER_SCREEN_HANDLER);
  }
}
