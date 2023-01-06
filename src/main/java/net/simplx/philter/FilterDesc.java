package net.simplx.philter;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;

public class FilterDesc {

  private static final String MATCHES = "Matches";
  private static final String MODE = "Mode";

  public FilterMode mode;
  public List<String> matchSpecs;

  public FilterDesc(FilterMode mode, List<String> matchSpecs) {
    this.mode = mode;
    this.matchSpecs = List.copyOf(matchSpecs);
  }

  public FilterDesc(PacketByteBuf buf) {
    mode = buf.readEnumConstant(FilterMode.class);
    matchSpecs = buf.readCollection(
        PacketByteBuf.getMaxValidator(Lists::newArrayListWithCapacity, FilterBlock.MAX_FILTERS),
        PacketByteBuf::readString);
  }

  public FilterDesc(NbtCompound nbt) {
    if (nbt.contains(MODE, NbtElement.INT_TYPE)) {
      mode = FilterMode.values()[nbt.getInt(MODE)];
    }

    if (!nbt.contains(MATCHES, NbtElement.STRING_TYPE)) {
      matchSpecs = Collections.emptyList();
    } else {
      NbtList matches = nbt.getList(MATCHES, NbtElement.STRING_TYPE);
      matchSpecs = new ArrayList<>(matches.size());
      for (int i = 0; i < matches.size(); i++) {
        matchSpecs.add(matches.getString(i));
      }
    }
  }

  public void writeNbt(NbtCompound nbt) {
    nbt.putInt(MODE, mode.ordinal());
    if (matchSpecs.size() > 0) {
      NbtList matches = new NbtList();
      for (String matchSpec : matchSpecs) {
        matches.add(NbtString.of(matchSpec));
      }
      nbt.put(MATCHES, matches);
    }
  }

  public void write(PacketByteBuf buf, BlockPos pos) {
    buf.writeEnumConstant(mode);
    buf.writeCollection(matchSpecs,
        (buf2, string) -> buf2.writeString(string, FilterBlock.MAX_FILTER_LEN));
    buf.writeBlockPos(pos);
  }
}
