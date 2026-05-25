package net.simplx.philter;

import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.Optional;

import static net.simplx.philter.PhilterMod.MOD_ID;

public record FilterData(FilterDesc desc, BlockPos pos, Direction facing, Direction filter,
                         Direction userFacing) implements CustomPacketPayload {
  public static final Type<FilterData> ID = new Type<>(Identifier.fromNamespaceAndPath(MOD_ID, "filter"));
  private static final StreamCodec<ByteBuf, Optional<Direction>> OPTIONAL_DIRECTION_CODEC =
      ByteBufCodecs.optional(Direction.STREAM_CODEC);
  private static final StreamCodec<ByteBuf, Direction> NULLABLE_DIRECTION_CODEC = new StreamCodec<>() {
    @Override
    public Direction decode(ByteBuf buf) {
      return OPTIONAL_DIRECTION_CODEC.decode(buf).orElse(null);
    }

    @Override
    public void encode(ByteBuf buf, Direction value) {
      OPTIONAL_DIRECTION_CODEC.encode(buf, Optional.ofNullable(value));
    }
  };
  public static final StreamCodec<RegistryFriendlyByteBuf, FilterData> STREAM_CODEC =
      StreamCodec.composite(FilterDesc.STREAM_CODEC, FilterData::desc, BlockPos.STREAM_CODEC, FilterData::pos,
          NULLABLE_DIRECTION_CODEC, FilterData::facing, NULLABLE_DIRECTION_CODEC, FilterData::filter,
          NULLABLE_DIRECTION_CODEC, FilterData::userFacing, FilterData::new);

  static {
    PayloadTypeRegistry.serverboundPlay().register(FilterData.ID, FilterData.STREAM_CODEC);
    ServerPlayNetworking.registerGlobalReceiver(FilterData.ID,
        (payload, context) -> FilterBlockEntity.updateEntity(context.player(), payload));
  }

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return ID;
  }
}
