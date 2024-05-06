package net.simplx.philter;

import com.google.common.collect.ImmutableList;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static net.simplx.philter.FilterMode.SAME_AS;

public class FilterDesc {

  public static final PacketCodec<ByteBuf, FilterDesc> PACKET_CODEC = new PacketCodec<>() {
    @Override
    public FilterDesc decode(ByteBuf buf) {
      try {
        return new FilterDesc(PacketCodecs.UNLIMITED_NBT_COMPOUND.decode(buf));
      } catch (IllegalArgumentException | NullPointerException e) {
        return new FilterDesc(new NbtCompound());
      }
    }

    @Override
    public void encode(ByteBuf buf, FilterDesc value) {
      PacketCodecs.UNLIMITED_NBT_COMPOUND.encode(buf, value.toNbt());
    }
  };
  private static final String MATCHES = "Matches";
  private static final String MODE = "Mode";
  private static final String EXACT = "Exact";
  private static final String MATCH_ALL = "MatchAll";

  public FilterMode mode;
  public ImmutableList<String> matches;
  public boolean exact;
  public boolean matchAll;

  public FilterDesc(FilterMode mode, List<String> matches, boolean exact) {
    this.mode = mode;
    this.matches = ImmutableList.copyOf(matches);
    this.exact = exact;
  }

  @SuppressWarnings("SimplifiableConditionalExpression")
  public FilterDesc(NbtCompound nbt) {
    try {
      mode = SAME_AS;
      if (nbt.contains(MODE, NbtElement.STRING_TYPE)) {
        mode = FilterMode.valueOf(nbt.getString(MODE));
      }
      if (!nbt.contains(MATCHES, NbtElement.LIST_TYPE)) {
        matches = ImmutableList.of();
      } else {
        NbtList nbtList = nbt.getList(MATCHES, NbtElement.STRING_TYPE);
        ImmutableList.Builder<String> matches = ImmutableList.builder();
        for (int i = 0; i < nbtList.size(); i++) {
          matches.add(nbtList.getString(i));
        }
        this.matches = matches.build();
      }
      matchAll = nbt.contains(MATCH_ALL, NbtElement.BYTE_TYPE) ? nbt.getBoolean(EXACT) : false;
      exact = nbt.contains(EXACT, NbtElement.BYTE_TYPE) ? nbt.getBoolean(EXACT) : false;
    } catch (IllegalArgumentException | NullPointerException e) {
      mode = SAME_AS;
      matches = ImmutableList.of();
      exact = true;
    }
  }

  public void writeNbt(NbtCompound nbt) {
    nbt.putString(MODE, mode.toString());
    if (matches.size() > 0) {
      NbtList nbtList = new NbtList();
      for (int i = 0; i < matches.size(); i++) {
        nbtList.add(NbtString.of(matches.get(i)));
      }
      nbt.put(MATCHES, nbtList);
      if (matchAll) {
        nbt.putBoolean(MATCH_ALL, true);
      }
    }
    nbt.putBoolean(EXACT, exact);
  }

  public String match(int index) {
    return index >= matches.size() ? "" : matches.get(index);
  }

  @NotNull
  private NbtCompound toNbt() {
    NbtCompound nbt = new NbtCompound();
    writeNbt(nbt);
    return nbt;
  }
}
