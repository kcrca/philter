package net.simplx.mcguidoc;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public class DocBlockEntity extends BlockEntity implements NamedScreenHandlerFactory {

  public DocBlockEntity(BlockPos pos, BlockState state) {
    super(MoodleMod.DOC_BLOCK_ENTITY, pos, state);
  }

  @Nullable
  @Override
  public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
    return new DocScreenHandler(syncId, inv, new MoodleMod.DummyData(0));
  }

  @Override
  public Text getDisplayName() {
    return Text.literal("McGUI Doc");
  }
}
