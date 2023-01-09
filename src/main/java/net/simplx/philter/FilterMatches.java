package net.simplx.philter;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.visitor.StringNbtWriter;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class FilterMatches {

  private static final Pattern SPEC = Pattern.compile("[^\\s,]+");

  public static class Spec {

    public final String spec;
    public final int start, end;

    public Spec(String spec, int start, int end) {
      this.spec = spec;
      this.start = start;
      this.end = end;
    }
  }

  public final String input;
  public final ImmutableList<Spec> specs;
  public final ImmutableList<Identifier> tagsYes;
  public final ImmutableList<Identifier> tagsNo;
  public final ImmutableList<Pattern> patternsYes;
  public final ImmutableList<Pattern> patternsNo;

  public FilterMatches(String input) {
    this.input = input;
    ImmutableList.Builder<Spec> specs = ImmutableList.builder();
    ImmutableList.Builder<Identifier> tagsYes = ImmutableList.builder();
    ImmutableList.Builder<Identifier> tagsNo = ImmutableList.builder();
    ImmutableList.Builder<Pattern> patternsYes = ImmutableList.builder();
    ImmutableList.Builder<Pattern> patternsNo = ImmutableList.builder();

    Matcher m = SPEC.matcher(input);
    while (m.find()) {
      String spec = m.group();
      specs.add(new Spec(spec, m.start(), m.end()));

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

    this.specs = specs.build();
    this.tagsYes = tagsYes.build();
    this.tagsNo = tagsNo.build();
    this.patternsYes = patternsYes.build();
    this.patternsNo = patternsNo.build();
  }

  public boolean matchAny(ItemStack item, boolean exact) {
    return checkTags(tagsYes, true, item, exact) || checkTags(tagsNo, false, item, exact)
        || checkPatterns(patternsYes, true, item, exact) || checkPatterns(patternsNo, false, item,
        exact);
  }

  private boolean checkTags(List<Identifier> tags, boolean yes, ItemStack item, boolean exact) {
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
