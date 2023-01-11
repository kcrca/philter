package net.simplx.philter;

import static net.minecraft.block.entity.HopperBlockEntity.INVENTORY_SIZE;
import static net.simplx.philter.PhilterMod.FILTER_SCREEN_HANDLER;

import java.lang.reflect.Field;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.HopperScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.math.BlockPos;

public class FilterScreenHandler extends HopperScreenHandler implements Forcer {

  public static final Field TYPE_F = Forcer.field(ScreenHandler.class, "type");

  private final FilterDesc filterDesc;
  private final BlockPos pos;

  public FilterScreenHandler(int syncId, PlayerInventory playerInventory, PacketByteBuf buf) {
    this(syncId, playerInventory, new SimpleInventory(INVENTORY_SIZE), new FilterDesc(buf),
        buf.readBlockPos());
  }

  public FilterScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory,
      FilterDesc filterDesc, BlockPos pos) {
    super(syncId, playerInventory, inventory);
    forceSet(TYPE_F, FILTER_SCREEN_HANDLER);
    this.filterDesc = filterDesc;
    this.pos = pos;
  }

  public FilterDesc getFilterDesc() {
    return filterDesc;
  }

  public BlockPos getPos() {
    return pos;
  }
}
