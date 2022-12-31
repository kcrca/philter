package net.simplx.philter;

import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.HopperBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.Hopper;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
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

  private final FilterAsHopperEntity hopperEntity;

  protected FilterBlockEntity(BlockPos pos, BlockState state) {
    super(Philter.FILTER_BLOCK_ENTITY, pos, state);
    hopperEntity = new FilterAsHopperEntity(pos, state);
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
    HopperBlockEntity.serverTick(world, pos, state, blockEntity.hopperEntity);
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
