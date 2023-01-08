package net.simplx.philter;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;

public class FilterDesc {

  private static final String MATCHES = "Matches";
  private static final String MODE = "Mode";
  private static final String EXACT = "Exact";
  static final int MATCHES_MAX_LEN = 150;

  public FilterMode mode;
  public String matches;
  public boolean exact;

  public FilterDesc(FilterMode mode, String matches) {
    this.mode = mode;
    this.matches = matches;
    exact = true;
  }

  public FilterDesc(PacketByteBuf buf) {
    mode = buf.readEnumConstant(FilterMode.class);
    matches = buf.readString(MATCHES_MAX_LEN);
    exact = buf.readBoolean();
  }

  public FilterDesc(NbtCompound nbt) {
    if (nbt.contains(MODE, NbtElement.STRING_TYPE)) {
      mode = FilterMode.valueOf(nbt.getString(MODE));
    }
    matches = nbt.getString(MATCHES);
    exact = nbt.contains(EXACT, NbtElement.BYTE_TYPE) ? nbt.getBoolean(EXACT) : true;
  }

  public void write(PacketByteBuf buf, BlockPos pos) {
    buf.writeEnumConstant(mode);
    buf.writeString(matches, MATCHES_MAX_LEN);
    buf.writeBoolean(exact);
    buf.writeBlockPos(pos);
  }

  public void writeNbt(NbtCompound nbt) {
    nbt.putString(MODE, mode.toString());
    if (matches.length() > 0) {
      nbt.putString(MATCHES, matches);
    }
    nbt.putBoolean(EXACT, exact);
  }
}
