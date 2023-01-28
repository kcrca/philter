package net.simplx.philter;

import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

public class FilterSlot extends Slot {

  private boolean enabled;
  private final boolean alwaysEnabled;

  public FilterSlot(Inventory inventory, int index, int x, int y, boolean alwaysEnabled) {
    super(inventory, index, x, y);
    this.alwaysEnabled = alwaysEnabled;
  }

  @Override
  public boolean isEnabled() {
    return alwaysEnabled || enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  @Override
  public boolean canInsert(ItemStack stack) {
    System.out.printf("enabled: %s\n", isEnabled());
    return isEnabled();
  }
}
