package net.simplx.philter;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;

public class FilterDesc {

  private static final String MATCHES = "Matches";
  private static final String MODE = "Mode";
  private static final String GENERAL = "General";
  static final int MATCHES_MAX_LEN = 150;

  public FilterMode mode;
  public String matchSpec;

  public FilterDesc(FilterMode mode, String matchSpec) {
    this.mode = mode;
    this.matchSpec = matchSpec;
  }

  public FilterDesc(PacketByteBuf buf) {
    mode = buf.readEnumConstant(FilterMode.class);
    matchSpec = buf.readString(MATCHES_MAX_LEN);
  }

  public FilterDesc(NbtCompound nbt) {
    if (nbt.contains(MODE, NbtElement.STRING_TYPE)) {
      mode = FilterMode.valueOf(nbt.getString(MODE));
    }
    matchSpec = nbt.getString(MATCHES);
  }

  public void writeNbt(NbtCompound nbt) {
    nbt.putString(MODE, mode.toString());
    if (matchSpec.length() > 0) {
      nbt.putString(MATCHES, matchSpec);
    }
  }

  public void write(PacketByteBuf buf, BlockPos pos) {
    buf.writeEnumConstant(mode);
    buf.writeString(matchSpec, MATCHES_MAX_LEN);
    buf.writeBlockPos(pos);
  }
}
