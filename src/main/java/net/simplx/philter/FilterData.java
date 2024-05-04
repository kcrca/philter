package net.simplx.philter;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import static net.simplx.philter.PhilterMod.FILTER_ID;

public record FilterData(FilterDesc desc, BlockPos pos, Direction facing, Direction filter,
                         Direction userFacing) implements CustomPayload {
  public static final Id<FilterData> ID = CustomPayload.id(FILTER_ID.getNamespace() + ":" + FILTER_ID.getPath());
  public static final PacketCodec<RegistryByteBuf, FilterData> PACKET_CODEC =
      PacketCodec.tuple(FilterDesc.PACKET_CODEC, FilterData::desc, BlockPos.PACKET_CODEC, FilterData::pos,
          Direction.PACKET_CODEC, FilterData::facing, Direction.PACKET_CODEC, FilterData::filter,
          Direction.PACKET_CODEC, FilterData::userFacing, FilterData::new);

  static {
  }

  @Override
  public Id<? extends CustomPayload> getId() {
    return ID;
  }
}
