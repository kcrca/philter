package net.simplx.philter;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.Hopper;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;

public class FilterBlockEntity extends LootableContainerBlockEntity implements Hopper {
  protected FilterBlockEntity(
      BlockEntityType<?> blockEntityType,
      BlockPos blockPos, BlockState blockState) {
    super(blockEntityType, blockPos, blockState);
  }

  @Override
  public double getHopperX() {
    return 0;
  }

  @Override
  public double getHopperY() {
    return 0;
  }

  @Override
  public double getHopperZ() {
    return 0;
  }

  @Override
  protected DefaultedList<ItemStack> getInvStackList() {
    return null;
  }

  @Override
  protected void setInvStackList(DefaultedList<ItemStack> list) {

  }

  @Override
  protected Text getContainerName() {
    return null;
  }

  @Override
  protected ScreenHandler createScreenHandler(int syncId, PlayerInventory playerInventory) {
    return null;
  }

  @Override
  public int size() {
    return 0;
  }
}
