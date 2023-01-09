package net.simplx.philter;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.visitor.StringNbtWriter;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

public class FilterMatches {


  public final ImmutableList<String> input;
  public final ImmutableList<Identifier> tagsYes;
  public final ImmutableList<Identifier> tagsNo;
  public final ImmutableList<Pattern> patternsYes;
  public final ImmutableList<Pattern> patternsNo;

  public FilterMatches(ImmutableList<String> input) {
    this.input = input;
    ImmutableList.Builder<Identifier> tagsYes = ImmutableList.builder();
    ImmutableList.Builder<Identifier> tagsNo = ImmutableList.builder();
    ImmutableList.Builder<Pattern> patternsYes = ImmutableList.builder();
    ImmutableList.Builder<Pattern> patternsNo = ImmutableList.builder();

    for (String spec : input) {
      var yes = spec.charAt(0) != '!';
      if (!yes) {
        spec = spec.substring(1);
      }
      if (spec.startsWith("#")) {
        spec = spec.substring(1);
        var id = Identifier.tryParse(spec);
        (yes ? tagsYes : tagsNo).add(id);
      } else {
        (yes ? patternsYes : patternsNo).add(Pattern.compile(spec));
      }
    }

    this.tagsYes = tagsYes.build();
    this.tagsNo = tagsNo.build();
    this.patternsYes = patternsYes.build();
    this.patternsNo = patternsNo.build();
  }

  public boolean matchAny(ItemStack item, boolean exact) {
    return checkTags(tagsYes, true, item) || checkTags(tagsNo, false, item) || checkPatterns(
        patternsYes, true, item, exact) || checkPatterns(patternsNo, false, item, exact);
  }

  private boolean checkTags(List<Identifier> tags, boolean yes, ItemStack item) {
    for (Identifier tag : tags) {
      TagKey<Item> t = TagKey.of(RegistryKeys.ITEM, tag);
      boolean isIn = item.isIn(t);
      if (isIn == yes) {
        return true;
      }
    }
    return false;
  }

  private boolean checkPatterns(List<Pattern> patterns, boolean yes, ItemStack item,
      boolean exact) {
    for (Pattern pattern : patterns) {
      Item it = item.getItem();
      Optional<RegistryKey<Item>> key = it.getRegistryEntry().getKey();
      if (key.isEmpty()) {
        continue;
      }
      Identifier id = key.get().getValue();
      String nbtStr = "";
      if (exact) {
        NbtCompound nbt = item.getNbt();
        if (nbt == null) {
          nbtStr = "{}";
        } else {
          nbtStr = new StringNbtWriter().apply(nbt);
        }
      }
      if (pattern.matcher(id.toString() + nbtStr).matches() == yes
          || id.getNamespace().equals("minecraft")
          && pattern.matcher(id.getPath() + nbtStr).matches() == yes) {
        return true;
      }
    }
    return false;
  }

}
