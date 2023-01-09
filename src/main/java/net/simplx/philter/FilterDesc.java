package net.simplx.philter;

import static java.util.Objects.requireNonNull;
import static net.simplx.philter.FilterMode.ONLY_SAME;

import com.google.common.collect.ImmutableList;
import java.util.List;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FilterDesc {

  private static final Logger LOGGER = LoggerFactory.getLogger(FilterDesc.class);

  private static final String MATCHES = "Matches";
  private static final String MODE = "Mode";
  private static final String EXACT = "Exact";
  static final int MATCHES_MAX_COUNT = 12;

  public FilterMode mode;
  public ImmutableList<String> matches;
  public boolean exact;

  public FilterDesc(FilterMode mode, List<String> matches, boolean exact) {
    if (matches.size() > MATCHES_MAX_COUNT) {
      LOGGER.warn(matches.size() + ": More than " + MATCHES_MAX_COUNT + " matches, truncated");
      matches = matches.subList(0, MATCHES_MAX_COUNT);
    }
    this.mode = mode;
    this.matches = ImmutableList.copyOf(matches);
    this.exact = exact;
  }

  public FilterDesc(PacketByteBuf buf) {
    this(requireNonNull(buf.readNbt()));
  }

  public FilterDesc(NbtCompound nbt) {
    try {
      mode = ONLY_SAME;
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
      //noinspection SimplifiableConditionalExpression
      exact = nbt.contains(EXACT, NbtElement.BYTE_TYPE) ? nbt.getBoolean(EXACT) : false;
    } catch (IllegalArgumentException | NullPointerException e) {
      mode = ONLY_SAME;
      matches = ImmutableList.of();
      exact = true;
    }
  }

  public String match(int index) {
    return index >= matches.size() ? "" : matches.get(index);
  }

  public void write(PacketByteBuf buf, BlockPos pos) {
    NbtCompound nbt = new NbtCompound();
    writeNbt(nbt);
    buf.writeNbt(nbt);
    buf.writeBlockPos(pos);
  }

  public void writeNbt(NbtCompound nbt) {
    nbt.putString(MODE, mode.toString());
    if (matches.size() > 0) {
      NbtList nbtList = new NbtList();
      for (int i = 0; i < matches.size(); i++) {
        nbtList.add(NbtString.of(matches.get(i)));
      }
      nbt.put(MATCHES, nbtList);
    }
    nbt.putBoolean(EXACT, exact);
  }
}
