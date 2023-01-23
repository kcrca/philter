package net.simplx.philter;

import static net.simplx.philter.FilterBlock.FACING;
import static net.simplx.philter.FilterBlock.FILTER;
import static net.simplx.philter.FilterBlock.FILTERED;

import com.google.common.collect.ImmutableList;
import java.util.function.BooleanSupplier;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HopperBlock;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;

/**
 * This implementation is ... suboptimal. This block is effectively a hopper, but neither
 * HopperVBlock nor HopperBlocKEntity are designed for subclasses. So this is a mash-up of forced
 * semi-inheritance and copies where needed. The alternative is to simply copy the entire hopper
 * entity class and tweak it. This way, at least what <em>can</em> be inherited is inherited.
 *
 * This is entirely to be able to re-write the insert() method to check the filter before doing any
 * move. The static serverTick() here simply invokes doServerTick() as an instance method, which
 * mirrors the static HopperBlockState.serverTick() (non-statically) and so on until we get to
 * insert(). Everything below that we just invoke the superclass method (honestly or dishonestly).
 *
 * This also requires handling entities dropped on top specially because of course it does; see
 * {@link FilterBlockEntity#onEntityCollided}.
 */
@SuppressWarnings("SameParameterValue")
public class FilterBlockEntity extends HopperBlockEntity implements ExtendedScreenHandlerFactory {

  private FilterDesc desc;
  private FilterMatches filterMatches;
  private int flicker;
  private Direction userFacingDir;

  protected FilterBlockEntity(BlockPos pos, BlockState state) {
    super(pos, state);
    type = PhilterMod.FILTER_BLOCK_ENTITY;
    desc = new FilterDesc(FilterMode.SAME_AS, ImmutableList.of(), false);
    filterMatches = new FilterMatches(ImmutableList.of());
    flicker = 0;
  }

  public static void updateEntity(PlayerEntity player, PacketByteBuf buf) {
    FilterDesc filterDesc = new FilterDesc(buf);
    BlockPos pos = buf.readBlockPos();
    var rawEntity = player.getWorld().getBlockEntity(pos);
    if (rawEntity instanceof FilterBlockEntity) {
      try {
        ((FilterBlockEntity) rawEntity).setFilterDesc(filterDesc);
        buf.readEnumConstant(Direction.class);
        Direction newFilterDir = buf.readEnumConstant(Direction.class);
        if (rawEntity.getCachedState().get(FILTER) != newFilterDir) {
          player.world.setBlockState(pos, rawEntity.getCachedState().with(FILTER, newFilterDir));
        }
        rawEntity.markDirty();
      } finally {
        buf.release();
      }
    }
  }

  @Override
  public void readNbt(NbtCompound nbt) {
    super.readNbt(nbt);
    desc = new FilterDesc(nbt);
  }

  @Override
  protected void writeNbt(NbtCompound nbt) {
    super.writeNbt(nbt);
    desc.writeNbt(nbt);
  }

  @Override
  protected Text getContainerName() {
    return Text.translatable("philter.filter.name");
  }

  public static void serverTick(World world, BlockPos pos, BlockState state,
      FilterBlockEntity blockEntity) {
    blockEntity.doServerTick(world, pos, state);
  }

  private void doServerTick(World world, BlockPos pos, BlockState state) {
    transferCooldown--;
    lastTickTime = world.getTime();
    if (!needsCooldown()) {
      setTransferCooldown(0);
      insertAndExtract(world, pos, state, () -> extract(world, this));
    }
    if (flicker > 0) {
      --flicker;
      int newState = flicker > 0 ? 1 : 0;
      int curState = state.get(FILTERED);
      if (newState != curState) {
        world.setBlockState(pos, state.with(FILTERED, newState), Block.NOTIFY_LISTENERS);
      }
    }
  }

  @SuppressWarnings("UnusedReturnValue")
  private boolean insertAndExtract(World world, BlockPos pos, BlockState state,
      BooleanSupplier booleanSupplier) {
    if (world.isClient) {
      return false;
    }
    if (!needsCooldown() && state.get(HopperBlock.ENABLED)) {
      boolean bl = false;
      if (!isEmpty()) {
        var filterState = state.with(FACING, state.get(FILTER));
        SimpleInventory filterInventory = new SimpleInventory(size());
        for (int i = 0; i < size(); i++) {
          if (inFilter(getStack(i), world, pos, state)) {
            filterInventory.setStack(i, getStack(i));
            world.setBlockState(pos, state.with(FILTERED, 1), Block.NOTIFY_LISTENERS);
            flicker = 8;
            break;
          }
        }
        if (!filterInventory.isEmpty() && insert(world, pos, filterState, this)) {
          bl = true;
        } else {
          bl = insert(world, pos, state, this);
        }
      }
      if (!isFull()) {
        bl |= booleanSupplier.getAsBoolean();
      }
      if (bl) {
        setTransferCooldown(8);
        HopperBlockEntity.markDirty(world, pos, state);
        return true;
      }
    }
    return false;
  }

  public void onEntityCollided(World world, BlockPos pos, BlockState state, Entity entity) {
    if (entity instanceof ItemEntity && VoxelShapes.matchesAnywhere(
        VoxelShapes.cuboid(entity.getBoundingBox().offset(-pos.getX(), -pos.getY(), -pos.getZ())),
        getInputAreaShape(), BooleanBiFunction.AND)) {
      insertAndExtract(world, pos, state,
          () -> HopperBlockEntity.extract(this, (ItemEntity) entity));
    }
  }

  private boolean inFilter(ItemStack hopperStack, World world, BlockPos pos, BlockState state) {
    if (hopperStack.getCount() == 0) {
      return false;
    }
    return switch (desc.mode) {
      case NONE -> false;
      case SAME_AS -> filterSameAs(hopperStack, world, pos, state);
      case MATCHES -> filterMatches(hopperStack);
    };
  }

  private boolean filterSameAs(ItemStack item, World world, BlockPos pos, BlockState state) {
    Direction direction = state.get(FILTER);
    Inventory inventory = getInventoryAt(world, pos.offset(direction));
    if (inventory == null) {
      return false;
    }
    for (int i = 0; i < inventory.size(); i++) {
      ItemStack invStack = inventory.getStack(i);
      if (desc.exact) {
        if (canMergeItems(invStack, item)) {
          return true;
        }
      } else {
        if (invStack.isItemEqual(item)) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean filterMatches(ItemStack item) {
    if (!filterMatches.input.equals(desc.matches)) {
      filterMatches = new FilterMatches(desc.matches);
    }
    return desc.matchAll ? filterMatches.matchAll(item, desc.exact, true)
        : filterMatches.matchAny(item, desc.exact, false);
  }

  @Override
  protected ScreenHandler createScreenHandler(int syncId, PlayerInventory playerInventory) {
    return new FilterScreenHandler(syncId, playerInventory, this, desc, pos,
        getCachedState().get(FACING), getCachedState().get(FILTER));
  }

  @Override
  public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
    BlockState state = player.getWorld().getBlockState(pos);
    Direction facing = state.get(FACING);
    Direction filter = state.get(FILTER);
    desc.write(buf, pos, facing, filter);
    buf.writeEnumConstant(userFacingDir);
  }

  public void setFilterDesc(FilterDesc desc) {
    this.desc = desc;
    markDirty();
  }

  public void setActionDir(Direction userFacingDir) {

    this.userFacingDir = userFacingDir;
  }
}
