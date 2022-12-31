package net.simplx.philter;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.stat.Stats;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class FilterBlock extends Block {

  public static final DirectionProperty FACING = Properties.HOPPER_FACING;
  public static final BooleanProperty ENABLED = Properties.ENABLED;
  public static final EnumProperty<FilterOut> FILTER_OUT = EnumProperty.of("filter_out",
      FilterOut.class);

  public FilterBlock(Settings settings) {
    super(settings);
    setDefaultState(
        stateManager.getDefaultState().with(FACING, Direction.DOWN).with(FILTER_OUT, FilterOut.LEFT)
            .with(ENABLED, true));
  }

  @Override
  protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
    builder.add(FACING, ENABLED, FILTER_OUT);
  }

  @Nullable
  @Override
  public BlockState getPlacementState(ItemPlacementContext ctx) {
    Direction direction = ctx.getSide().getOpposite();
    return getDefaultState().with(FACING,
        direction.getAxis() == Axis.Y ? Direction.DOWN : direction).with(ENABLED, true);
  }


  @Override
  public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player,
      Hand hand, BlockHitResult hit) {
    if (world.isClient) {
      return ActionResult.SUCCESS;
    } else {
      BlockEntity blockEntity = world.getBlockEntity(pos);
      if (blockEntity instanceof FilterBlockEntity) {
        player.openHandledScreen((FilterBlockEntity) blockEntity);
        player.incrementStat(Stats.INSPECT_HOPPER);
      }
      return ActionResult.CONSUME;
    }
  }
}
