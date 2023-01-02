package net.simplx.philter;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.HopperBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.Hopper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.stat.Stats;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class FilterBlock extends HopperBlock {

  public static final DirectionProperty FACING = Properties.HOPPER_FACING;
  public static final BooleanProperty ENABLED = Properties.ENABLED;
  public static final DirectionProperty FILTER = DirectionProperty.of("filter");

  private static final VoxelShape CENTER_SHAPE = Block.createCuboidShape(4, 4, 4, 12, 12, 12);
  private static final Map<Direction, VoxelShape> POINTING = new EnumMap<>(Direction.class);
  private static final Map<Direction, Map<Direction, VoxelShape>> SHAPES = new EnumMap<>(
      Direction.class);
  private static final Map<Direction, Map<Direction, VoxelShape>> RAYCAST_SHAPES = new EnumMap<>(
      Direction.class);

  static {
    POINTING.put(Direction.NORTH, Block.createCuboidShape(6, 6, 0, 10, 10, 4));
    POINTING.put(Direction.SOUTH, Block.createCuboidShape(12, 6, 6, 16, 10, 10));
    POINTING.put(Direction.EAST, Block.createCuboidShape(12, 6, 6, 16, 10, 10));
    POINTING.put(Direction.WEST, Block.createCuboidShape(0, 6, 6, 4, 10, 10));
    POINTING.put(Direction.UP, Block.createCuboidShape(6, 12, 6, 10, 16, 10));
    POINTING.put(Direction.DOWN, Block.createCuboidShape(6, 0, 6, 10, 4, 10));

    for (Direction facing : Direction.values()) {
      Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
      Map<Direction, VoxelShape> raycastShapes = new EnumMap<>(Direction.class);
      SHAPES.put(facing, shapes);
      RAYCAST_SHAPES.put(facing, raycastShapes);
      for (Direction filter : Direction.values()) {
        VoxelShape pointers = VoxelShapes.combineAndSimplify(POINTING.get(facing),
            POINTING.get(filter), BooleanBiFunction.OR);
        shapes.put(filter,
            VoxelShapes.combineAndSimplify(pointers, CENTER_SHAPE, BooleanBiFunction.OR));
        raycastShapes.put(filter,
            VoxelShapes.combineAndSimplify(pointers, Hopper.INSIDE_SHAPE, BooleanBiFunction.OR));
      }
    }
  }

  public FilterBlock(Settings settings) {
    super(settings);
    stateManager.getDefaultState();
    setDefaultState(getDefaultState().with(FILTER, Direction.NORTH));
  }

  @Nullable
  @Override
  public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
    return new FilterBlockEntity(pos, state);
  }

  @Override
  @Nullable
  public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state,
      BlockEntityType<T> type) {
    return world.isClient ? null
        : checkType(type, Philter.FILTER_BLOCK_ENTITY, FilterBlockEntity::serverTick);
  }


  @Override
  public boolean canPathfindThrough(BlockState state, BlockView world, BlockPos pos,
      NavigationType type) {
    return false;
  }

  @Override
  protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
    builder.add(FACING, ENABLED, FILTER);
  }

  @Override
  public BlockRenderType getRenderType(BlockState state) {
    return BlockRenderType.MODEL;
  }

  @Nullable
  @Override
  public BlockState getPlacementState(ItemPlacementContext ctx) {
    Direction direction = ctx.getSide().getOpposite();
    Direction facing = direction.getAxis() == Axis.Y ? Direction.DOWN : direction;
    Direction filter = Direction.NORTH;
    switch (facing) {
      case NORTH -> filter = Direction.EAST;
      case EAST -> filter = Direction.SOUTH;
      case SOUTH -> filter = Direction.WEST;
      case WEST -> filter = Direction.NORTH;
    }
    return getDefaultState().with(FACING, facing).with(FILTER, filter).with(ENABLED, true);
  }

  @Override
  public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos,
      ShapeContext context) {
    return SHAPES.get(state.get(FACING)).get(state.get(FILTER));
  }

  @Override
  public VoxelShape getRaycastShape(BlockState state, BlockView world, BlockPos pos) {
    return RAYCAST_SHAPES.get(state.get(FACING)).get(state.get(FILTER));
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

  @Override
  public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
    BlockEntity blockEntity = world.getBlockEntity(pos);
    if (blockEntity instanceof FilterBlockEntity) {
      ((FilterBlockEntity) blockEntity).onEntityCollided(world, pos, state, entity);
    }
  }

}
