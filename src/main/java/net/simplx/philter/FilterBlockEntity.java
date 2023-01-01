package net.simplx.philter;

import static net.simplx.philter.FilterBlock.FACING;
import static net.simplx.philter.FilterBlock.FILTER;

import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.Hopper;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class FilterBlockEntity extends LootableContainerBlockEntity implements
    NamedScreenHandlerFactory, Inventory, Hopper {

  private static final int STACK_SIZE = 5;
  private static final DefaultedList<ItemStack> EMPTY_STACK_LIST = DefaultedList.ofSize(STACK_SIZE,
      ItemStack.EMPTY);

  private final FilterAsHopperEntity hopperEntity;
  private final FilterAsHopperEntity filterEntity;
  private final BlockState filterState;

  private static class FilterAsHopperEntity extends HopperBlockEntity {

    public FilterAsHopperEntity(BlockPos pos, BlockState state) {
      super(pos, state);
    }

    @Override
    public void writeNbt(NbtCompound nbt) {
      super.writeNbt(nbt);
    }

    @Override
    protected DefaultedList<ItemStack> getInvStackList() {
      return super.getInvStackList();
    }

    @Override
    protected void setInvStackList(DefaultedList<ItemStack> list) {
      super.setInvStackList(list);
    }
  }

  protected FilterBlockEntity(BlockPos pos, BlockState state) {
    super(Philter.FILTER_BLOCK_ENTITY, pos, state);
    hopperEntity = new FilterAsHopperEntity(pos, state);
    filterState = state.with(FACING, state.get(FILTER));
    filterEntity = new FilterAsHopperEntity(pos, filterState);
    if (hopperEntity.size() != STACK_SIZE) {
      throw new IllegalStateException("Unexpected stack size: " + hopperEntity.size());
    }
  }

  @Override
  protected Text getContainerName() {
    return Text.translatable("container.filter");
  }


  @Override
  protected ScreenHandler createScreenHandler(int syncId, PlayerInventory playerInventory) {
    return null;
  }

  public VoxelShape getInputAreaShape() {
    return hopperEntity.getInputAreaShape();
  }

  @Override
  public void readNbt(NbtCompound nbt) {
    hopperEntity.readNbt(nbt);
  }

  @Override
  public void writeNbt(NbtCompound nbt) {
    hopperEntity.writeNbt(nbt);
  }

  @Override
  public int size() {
    return hopperEntity.size();
  }

  @Override
  public ItemStack removeStack(int slot, int amount) {
    return hopperEntity.removeStack(slot, amount);
  }

  @Override
  public void setStack(int slot, ItemStack stack) {
    hopperEntity.setStack(slot, stack);
  }

  public static void serverTick(World world, BlockPos pos, BlockState state,
      FilterBlockEntity blockEntity) {
    // We need to call (non-filtered) hopper whether or not the filter happened in order to keep the
    // cooldown values consistent (which are private so we can't just set them to avoid this). So if
    // the filter delivered an item, we set the hopper to have nothing, invoke it, and then set it
    // back to what it was.
    DefaultedList<ItemStack> inv = null;
    if (blockEntity.tryFilter(world)) {
      inv = blockEntity.hopperEntity.getInvStackList();
      blockEntity.setInvStackList(EMPTY_STACK_LIST);
    }
    HopperBlockEntity.serverTick(world, pos, state, blockEntity.hopperEntity);
    if (inv != null) {
      blockEntity.setInvStackList(inv);
    }
  }

  private boolean tryFilter(World world) {
    if (world.isClient) {
      return false;
    }

    // Make the stacks that match the filter managed by the filter "hopper"
    DefaultedList<ItemStack> hopperInv = hopperEntity.getInvStackList();
    DefaultedList<ItemStack> filterInv = DefaultedList.ofSize(size(), ItemStack.EMPTY);
    int[] counts = new int[size()];
    boolean candidates = false;
    for (int i = 0; i < size(); i++) {
      ItemStack hopperStack = hopperInv.get(i);
      if (!inFilter(hopperStack)) {
        counts[i] = 0;
        filterInv.set(i, ItemStack.EMPTY);
      } else {
        filterInv.set(i, hopperStack);
        counts[i] = hopperStack.getCount();
        candidates = true;
      }
    }

    // Try to push candidates in the filter direction.
    // We try even if there are no possible items to match the filter. This keeps the cooldown value
    // of the filter's entity at the right value.
    filterEntity.setInvStackList(filterInv);
    HopperBlockEntity.serverTick(world, pos, filterState, filterEntity);

    // See if anything changed, if so, then the filter was successful
    for (int i = 0; i < size(); i++) {
      if (filterInv.get(i).getCount() != counts[i]) {
        return true;
      }
    }
    return false;
  }

  private boolean inFilter(ItemStack hopperStack) {
    if (hopperStack.getCount() == 0) {
      return false;
    }
    Item item = hopperStack.getItem();
    return item.getName().getContent().toString().contains("sand");
  }

  public static boolean extract(World world, Hopper hopper) {
    return HopperBlockEntity.extract(world, hopper);
  }

  public static boolean extract(Inventory inventory, ItemEntity itemEntity) {
    return HopperBlockEntity.extract(inventory, itemEntity);
  }

  public static List<ItemEntity> getInputItemEntities(World world, Hopper hopper) {
    return HopperBlockEntity.getInputItemEntities(world, hopper);
  }

  @Nullable
  public static Inventory getInventoryAt(World world, BlockPos pos) {
    return HopperBlockEntity.getInventoryAt(world, pos);
  }

  public static ItemStack transfer(@Nullable Inventory from, Inventory to, ItemStack stack,
      @Nullable Direction side) {
    return HopperBlockEntity.transfer(from, to, stack, side);
  }

  public double getHopperX() {
    return hopperEntity.getHopperX();
  }

  public double getHopperY() {
    return hopperEntity.getHopperY();
  }

  public double getHopperZ() {
    return hopperEntity.getHopperZ();
  }

  @Override
  public DefaultedList<ItemStack> getInvStackList() {
    return hopperEntity.getInvStackList();
  }

  @Override
  public void setInvStackList(DefaultedList<ItemStack> list) {
    hopperEntity.setInvStackList(list);
  }
}
