package net.simplx.philter;

import static net.simplx.philter.PhilterMod.FILTER_ID;

import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.listener.ServerPlayPacketListener;
import net.minecraft.network.packet.c2s.play.CustomPayloadC2SPacket;
import net.minecraft.util.math.BlockPos;

public class FilterPacket extends CustomPayloadC2SPacket {

  public FilterPacket(FilterDesc desc, BlockPos pos) {
    super(FILTER_ID, packetBuf(desc, pos));
  }

  private static PacketByteBuf packetBuf(FilterDesc desc, BlockPos pos) {
    var buf = new PacketByteBuf(Unpooled.buffer());
    desc.write(buf, pos);
    return buf;
  }

  @Override
  public void apply(ServerPlayPacketListener listener) {
    listener.onCustomPayload(this);
  }
}
