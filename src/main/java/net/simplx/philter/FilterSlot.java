package net.simplx.philter;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.Slot;

public class FilterSlot extends Slot {

  private boolean enabled;
  private final boolean alwaysEnabled;

  public FilterSlot(Container inventory, int index, int x, int y, boolean alwaysEnabled) {
    super(inventory, index, x, y);
    this.alwaysEnabled = alwaysEnabled;
  }

  @Override
  public boolean isActive() {
    return alwaysEnabled || enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  @Override
  public int getMaxStackSize() {
    return 1;
  }

  @Override
  public boolean mayPlace(ItemStack stack) {
    return isActive();
  }
}
