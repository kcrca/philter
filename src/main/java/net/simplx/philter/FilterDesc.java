package net.simplx.philter;

import com.google.common.collect.ImmutableList;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static net.simplx.philter.FilterMode.SAME_AS;

public class FilterDesc {

  public static final StreamCodec<ByteBuf, FilterDesc> STREAM_CODEC = new StreamCodec<>() {
    @Override
    public FilterDesc decode(ByteBuf buf) {
      try {
        return new FilterDesc(ByteBufCodecs.COMPOUND_TAG.decode(buf));
      } catch (IllegalArgumentException | NullPointerException e) {
        return new FilterDesc(new CompoundTag());
      }
    }

    @Override
    public void encode(ByteBuf buf, FilterDesc value) {
      ByteBufCodecs.COMPOUND_TAG.encode(buf, value.toCompoundTag());
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
  public FilterDesc(CompoundTag nbt) {
    try {
      mode = SAME_AS;
      nbt.getString(MODE).ifPresent(s -> mode = FilterMode.valueOf(s));
      ListTag nbtList = nbt.getListOrEmpty(MATCHES);
      if (nbtList.isEmpty()) {
        matches = ImmutableList.of();
      } else {
        ImmutableList.Builder<String> matchesBuilder = ImmutableList.builder();
        for (int i = 0; i < nbtList.size(); i++) {
          nbtList.getString(i).ifPresent(matchesBuilder::add);
        }
        this.matches = matchesBuilder.build();
      }
      matchAll = nbt.getBooleanOr(MATCH_ALL, false);
      exact = nbt.getBooleanOr(EXACT, false);
    } catch (IllegalArgumentException | NullPointerException e) {
      mode = SAME_AS;
      matches = ImmutableList.of();
      exact = true;
    }
  }

  public FilterDesc(ValueInput input) {
    try {
      mode = SAME_AS;
      input.getString(MODE).ifPresent(s -> mode = FilterMode.valueOf(s));
      matches = ImmutableList.copyOf(
          input.listOrEmpty(MATCHES, com.mojang.serialization.Codec.STRING).stream().toList());
      matchAll = input.getBooleanOr(MATCH_ALL, false);
      exact = input.getBooleanOr(EXACT, false);
    } catch (IllegalArgumentException | NullPointerException e) {
      mode = SAME_AS;
      matches = ImmutableList.of();
      exact = true;
    }
  }

  public void write(ValueOutput output) {
    output.putString(MODE, mode.toString());
    if (!matches.isEmpty()) {
      var list = output.list(MATCHES, com.mojang.serialization.Codec.STRING);
      matches.forEach(list::add);
      if (matchAll) {
        output.putBoolean(MATCH_ALL, true);
      }
    }
    output.putBoolean(EXACT, exact);
  }

  public String match(int index) {
    return index >= matches.size() ? "" : matches.get(index);
  }

  @NotNull
  public CompoundTag toCompoundTag() {
    CompoundTag nbt = new CompoundTag();
    nbt.putString(MODE, mode.toString());
    if (!matches.isEmpty()) {
      ListTag nbtList = new ListTag();
      for (String match : matches) {
        nbtList.add(StringTag.valueOf(match));
      }
      nbt.put(MATCHES, nbtList);
      if (matchAll) {
        nbt.putBoolean(MATCH_ALL, true);
      }
    }
    nbt.putBoolean(EXACT, exact);
    return nbt;
  }
}
