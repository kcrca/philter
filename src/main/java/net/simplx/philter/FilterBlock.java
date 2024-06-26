package net.simplx.philter;

import com.google.common.collect.Iterators;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

import static net.minecraft.util.function.BooleanBiFunction.OR;

public class FilterBlock extends HopperBlock {
  public static final DirectionProperty FACING = Properties.HOPPER_FACING;
  public static final BooleanProperty ENABLED = Properties.ENABLED;
  public static final DirectionProperty FILTER = DirectionProperty.of("filter");
  public static final IntProperty FILTERED = IntProperty.of("filtered", 0, 1);

  private static final VoxelShape CENTER_SHAPE = Block.createCuboidShape(4, 4, 4, 12, 12, 12);
  private static final Map<Direction, Map<Direction, VoxelShape>> SHAPES = new EnumMap<>(
      Direction.class);
  private static final Map<Direction, Map<Direction, VoxelShape>> RAYCAST_SHAPES = new EnumMap<>(
      Direction.class);

  static {
    Map<Direction, VoxelShape> dirs = new EnumMap<>(Direction.class);
    dirs.put(Direction.DOWN, DOWN_SHAPE);
    dirs.put(Direction.UP, DOWN_SHAPE); // not used, but fails if nothing is set
    dirs.put(Direction.EAST, EAST_SHAPE);
    dirs.put(Direction.NORTH, NORTH_SHAPE);
    dirs.put(Direction.SOUTH, SOUTH_SHAPE);
    dirs.put(Direction.WEST, WEST_SHAPE);
    Map<Direction, VoxelShape> raycast = new EnumMap<>(Direction.class);
    raycast.put(Direction.DOWN, DOWN_RAYCAST_SHAPE);
    raycast.put(Direction.UP, DOWN_RAYCAST_SHAPE); // not used, but fails if nothing is set
    raycast.put(Direction.EAST, EAST_RAYCAST_SHAPE);
    raycast.put(Direction.NORTH, NORTH_RAYCAST_SHAPE);
    raycast.put(Direction.SOUTH, SOUTH_RAYCAST_SHAPE);
    raycast.put(Direction.WEST, WEST_RAYCAST_SHAPE);

    // Now we fill in the top of the hopper, where the sorter machine lives
    var voxels = Iterators.concat(dirs.entrySet().iterator(), raycast.entrySet().iterator());
    while (voxels.hasNext()) {
      var shape = voxels.next();
//      shape.setValue(VoxelShapes.combineAndSimplify(shape.getValue(), HopperBlock.INSIDE_SHAPE, OR));
      shape.setValue(shape.getValue());
    }

    for (Direction facing : Direction.values()) {
      Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
      Map<Direction, VoxelShape> raycastShapes = new EnumMap<>(Direction.class);
      SHAPES.put(facing, shapes);
      RAYCAST_SHAPES.put(facing, raycastShapes);
      for (Direction filter : Direction.values()) {
        VoxelShape p = VoxelShapes.combineAndSimplify(dirs.get(facing), dirs.get(filter), OR);
        shapes.put(filter, VoxelShapes.combineAndSimplify(p, CENTER_SHAPE, OR));
        VoxelShape r = VoxelShapes.combineAndSimplify(raycast.get(facing), raycast.get(filter), OR);
        raycastShapes.put(filter, VoxelShapes.combineAndSimplify(r, TOP_SHAPE, OR));
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
        : HopperBlock.validateTicker(type, PhilterMod.FILTER_BLOCK_ENTITY, FilterBlockEntity::serverTick);
  }

  @Override
  protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
    builder.add(FACING, ENABLED, FILTER, FILTERED);
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
    Direction[] directions = ctx.getPlacementDirections();
    for (int i = 1; i < directions.length; i++) {
      Direction filter = directions[i];
      if (filter != Direction.UP && filter != facing) {
        return getDefaultState().with(FACING, facing).with(FILTER, filter).with(ENABLED, true)
            .with(FILTERED, 0);
      }
    }
    throw new IllegalStateException(
        "No valid filter direction found in " + Arrays.toString(directions));
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
                            BlockHitResult hit) {
    if (world.isClient) {
      return ActionResult.SUCCESS;
    } else {
      BlockEntity blockEntity = world.getBlockEntity(pos);
      if (blockEntity instanceof FilterBlockEntity fbe) {
        Hand hand = player.getActiveHand();
        ItemPlacementContext ctx = new ItemPlacementContext(player, hand, ItemStack.EMPTY, hit);
        fbe.setActionDir(null);
        for (var dir : ctx.getPlacementDirections()) {
          if (dir.getAxis() != Axis.Y) {
            fbe.setActionDir(dir);
            break;
          }
        }
        player.openHandledScreen(fbe);
      }
      return ActionResult.CONSUME;
    }
  }
}
