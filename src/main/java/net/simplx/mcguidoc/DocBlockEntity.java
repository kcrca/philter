package net.simplx.mcguidoc;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class DocBlockEntity extends BlockEntity implements ExtendedMenuProvider<MoodleMod.DummyData> {

  public DocBlockEntity(BlockPos pos, BlockState state) {
    super(MoodleMod.DOC_BLOCK_ENTITY, pos, state);
  }

  @Override
  public MoodleMod.DummyData getScreenOpeningData(ServerPlayer player) {
    return new MoodleMod.DummyData(0);
  }

  @Override
  public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player player) {
    return new DocScreenHandler(syncId, inv, new MoodleMod.DummyData(0));
  }

  @Override
  public Component getDisplayName() {
    return Component.literal("McGUI Doc");
  }
}
