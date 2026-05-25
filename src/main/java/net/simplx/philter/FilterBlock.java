package net.simplx.philter;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

public class FilterBlock extends HopperBlock {
  public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING_HOPPER;
  public static final BooleanProperty ENABLED = BlockStateProperties.ENABLED;
  public static final EnumProperty<Direction> FILTER = EnumProperty.create("filter", Direction.class);
  public static final IntegerProperty FILTERED = IntegerProperty.create("filtered", 0, 1);

  private static final VoxelShape HOPPER_TOP = box(0, 10, 0, 16, 16, 16);
  private static final VoxelShape HOPPER_BODY = Shapes.or(HOPPER_TOP, box(4, 4, 4, 12, 10, 12));
  private static final VoxelShape HOPPER_INSIDE = box(2, 11, 2, 14, 16, 14);
  private static final VoxelShape CENTER_SHAPE = box(4, 4, 4, 12, 12, 12);

  private static final Map<Direction, Map<Direction, VoxelShape>> SHAPES = new EnumMap<>(Direction.class);
  private static final Map<Direction, Map<Direction, VoxelShape>> RAYCAST_SHAPES = new EnumMap<>(Direction.class);

  static {
    for (Direction facing : Direction.values()) {
      Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
      Map<Direction, VoxelShape> raycastShapes = new EnumMap<>(Direction.class);
      SHAPES.put(facing, shapes);
      RAYCAST_SHAPES.put(facing, raycastShapes);
      VoxelShape facingShape = Shapes.or(HOPPER_BODY, pipeShape(facing));
      VoxelShape facingInside = Shapes.or(HOPPER_INSIDE, pipeShape(facing));
      for (Direction filter : Direction.values()) {
        VoxelShape p = Shapes.joinUnoptimized(facingShape, Shapes.or(HOPPER_BODY, pipeShape(filter)), BooleanOp.OR);
        shapes.put(filter, Shapes.joinUnoptimized(p, CENTER_SHAPE, BooleanOp.OR));
        VoxelShape r = Shapes.joinUnoptimized(facingInside, Shapes.or(HOPPER_INSIDE, pipeShape(filter)), BooleanOp.OR);
        raycastShapes.put(filter, Shapes.joinUnoptimized(r, HOPPER_TOP, BooleanOp.OR));
      }
    }
  }

  private static VoxelShape pipeShape(Direction dir) {
    return switch (dir) {
      case DOWN -> box(6, 0, 6, 10, 4, 10);
      case UP -> box(6, 4, 6, 10, 10, 10);
      case NORTH -> box(6, 6, 0, 10, 10, 4);
      case SOUTH -> box(6, 6, 12, 10, 10, 16);
      case WEST -> box(0, 6, 6, 4, 10, 10);
      case EAST -> box(12, 6, 6, 16, 10, 10);
    };
  }

  public FilterBlock(BlockBehaviour.Properties properties) {
    super(properties);
    registerDefaultState(defaultBlockState().setValue(FILTER, Direction.NORTH));
  }

  @Nullable
  @Override
  public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
    return new FilterBlockEntity(pos, state);
  }

  @Override
  @Nullable
  public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                BlockEntityType<T> type) {
    return level.isClientSide() ? null
        : createTickerHelper(type, PhilterMod.FILTER_BLOCK_ENTITY, FilterBlockEntity::serverTick);
  }

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
    builder.add(FACING, ENABLED, FILTER, FILTERED);
  }

  @Override
  protected RenderShape getRenderShape(BlockState state) {
    return RenderShape.MODEL;
  }

  @Nullable
  @Override
  public BlockState getStateForPlacement(BlockPlaceContext ctx) {
    Direction direction = ctx.getClickedFace().getOpposite();
    Direction facing = direction.getAxis() == Axis.Y ? Direction.DOWN : direction;
    Direction[] directions = ctx.getNearestLookingDirections();
    for (int i = 1; i < directions.length; i++) {
      Direction filter = directions[i];
      if (filter != Direction.UP && filter != facing) {
        return defaultBlockState().setValue(FACING, facing).setValue(FILTER, filter).setValue(ENABLED, true)
            .setValue(FILTERED, 0);
      }
    }
    throw new IllegalStateException(
        "No valid filter direction found in " + Arrays.toString(directions));
  }

  @Override
  protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos,
                                CollisionContext context) {
    return SHAPES.get(state.getValue(FACING)).get(state.getValue(FILTER));
  }

  @Override
  protected VoxelShape getInteractionShape(BlockState state, BlockGetter world, BlockPos pos) {
    return RAYCAST_SHAPES.get(state.getValue(FACING)).get(state.getValue(FILTER));
  }

  @Override
  protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                            BlockHitResult hit) {
    if (level.isClientSide()) {
      return InteractionResult.SUCCESS;
    } else {
      BlockEntity blockEntity = level.getBlockEntity(pos);
      if (blockEntity instanceof FilterBlockEntity fbe) {
        BlockPlaceContext ctx = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, ItemStack.EMPTY, hit);
        fbe.setActionDir(null);
        for (var dir : ctx.getNearestLookingDirections()) {
          if (dir.getAxis() != Axis.Y) {
            fbe.setActionDir(dir);
            break;
          }
        }
        player.openMenu(fbe);
      }
      return InteractionResult.CONSUME;
    }
  }
}
