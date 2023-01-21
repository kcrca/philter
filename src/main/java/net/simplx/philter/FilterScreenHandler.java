package net.simplx.philter;

import static net.minecraft.block.entity.HopperBlockEntity.INVENTORY_SIZE;
import static net.simplx.philter.PhilterMod.FILTER_SCREEN_HANDLER;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.HopperScreenHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class FilterScreenHandler extends HopperScreenHandler {

  private final FilterDesc filterDesc;
  final BlockPos pos;
  final Direction facing;
  Direction userFacingDir;
  Direction filter;

  public FilterScreenHandler(int syncId, PlayerInventory playerInventory, PacketByteBuf buf) {
    this(syncId, playerInventory, new SimpleInventory(INVENTORY_SIZE), new FilterDesc(buf),
        buf.readBlockPos(), buf.readEnumConstant(Direction.class),
        buf.readEnumConstant(Direction.class));
    userFacingDir = buf.readEnumConstant(Direction.class);
  }

  public FilterScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory,
      FilterDesc filterDesc, BlockPos pos, Direction facing, Direction filter) {
    super(syncId, playerInventory, inventory);
    type = FILTER_SCREEN_HANDLER;
    this.filterDesc = filterDesc;
    this.pos = pos;
    this.facing = facing;
    this.filter = filter;
  }

  public FilterDesc getFilterDesc() {
    return filterDesc;
  }

  public BlockPos getPos() {
    return pos;
  }
}
