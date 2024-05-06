package net.simplx.philter;

import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Optional;

import static net.simplx.philter.PhilterMod.FILTER_ID;

public record FilterData(FilterDesc desc, BlockPos pos, Direction facing, Direction filter,
                         Direction userFacing) implements CustomPayload {
  public static final Id<FilterData> ID = CustomPayload.id(FILTER_ID.getNamespace() + ":" + FILTER_ID.getPath());
  private static final PacketCodec<ByteBuf, Optional<Direction>> OPTIONAL_DIRECTION_PACKET_CODEC =
      PacketCodecs.optional(Direction.PACKET_CODEC);
  private static final PacketCodec<ByteBuf, Direction> NULLABLE_DIRECTION_CODEC = new PacketCodec<>() {
    @Override
    public Direction decode(ByteBuf buf) {
      return OPTIONAL_DIRECTION_PACKET_CODEC.decode(buf).orElse(null);
    }

    @Override
    public void encode(ByteBuf buf, Direction value) {
      OPTIONAL_DIRECTION_PACKET_CODEC.encode(buf, Optional.ofNullable(value));
    }
  };
  public static final PacketCodec<RegistryByteBuf, FilterData> PACKET_CODEC =
      PacketCodec.tuple(FilterDesc.PACKET_CODEC, FilterData::desc, BlockPos.PACKET_CODEC, FilterData::pos,
          NULLABLE_DIRECTION_CODEC, FilterData::facing, NULLABLE_DIRECTION_CODEC, FilterData::filter,
          NULLABLE_DIRECTION_CODEC, FilterData::userFacing, FilterData::new);

  static {
    PayloadTypeRegistry.playC2S().register(FilterData.ID, FilterData.PACKET_CODEC);
    ServerPlayNetworking.registerGlobalReceiver(FilterData.ID,
        (payload, context) -> FilterBlockEntity.updateEntity(context.player(), payload));
  }

  @Override
  public Id<? extends CustomPayload> getId() {
    return ID;
  }
}
