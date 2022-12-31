package net.simplx.philter;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public class FilterBlockEntity extends BlockEntity implements NamedScreenHandlerFactory {

  protected FilterBlockEntity(
      BlockEntityType<?> blockEntityType,
      BlockPos blockPos, BlockState blockState) {
    super(blockEntityType, blockPos, blockState);
  }

  @Override
  public Text getDisplayName() {
    return null;
  }

  @Nullable
  @Override
  public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
    return null;
  }
}
