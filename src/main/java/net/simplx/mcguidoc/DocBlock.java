package net.simplx.mcguidoc;

import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class DocBlock extends BlockWithEntity {

  public DocBlock(Settings settings) {
    super(settings);
    stateManager.getDefaultState();
  }

  @Nullable
  @Override
  public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
    return new DocBlockEntity(pos, state);
  }

  @Override
  public BlockRenderType getRenderType(BlockState state) {
    return BlockRenderType.MODEL;
  }

  @Override
  public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player,
      Hand hand, BlockHitResult hit) {
    if (!world.isClient) {
      NamedScreenHandlerFactory factory = state.createScreenHandlerFactory(world, pos);
      if (factory != null) {
        //With this call the server will request the client to open the appropriate Screenhandler
        player.openHandledScreen(factory);
      }
    }
    return ActionResult.SUCCESS;
  }
}
