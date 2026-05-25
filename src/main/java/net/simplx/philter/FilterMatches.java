package net.simplx.philter;

import com.google.common.collect.ImmutableList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

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
        if (id != null) {
          (yes ? tagsYes : tagsNo).add(id);
        }
      } else {
        (yes ? patternsYes : patternsNo).add(Pattern.compile(spec, Pattern.CASE_INSENSITIVE));
      }
    }

    this.tagsYes = tagsYes.build();
    this.tagsNo = tagsNo.build();
    this.patternsYes = patternsYes.build();
    this.patternsNo = patternsNo.build();
  }

  public boolean matchAny(ItemStack item, boolean exact, boolean matchAll) {
    return checkTags(tagsYes, true, item, matchAll) || checkTags(tagsNo, false, item, matchAll)
        || checkPatterns(patternsYes, true, item, exact, matchAll)
        || checkPatterns(patternsNo, false, item, exact, matchAll);
  }

  public boolean matchAll(ItemStack item, boolean exact, boolean matchAll) {
    return checkTags(tagsYes, true, item, matchAll) && checkTags(tagsNo, false, item, matchAll)
        && checkPatterns(patternsYes, true, item, exact, matchAll)
        && checkPatterns(patternsNo, false, item, exact, matchAll);
  }

  private boolean checkTags(List<Identifier> tags, boolean yes, ItemStack item, boolean matchAll) {
    for (Identifier tag : tags) {
      TagKey<Item> t = TagKey.create(Registries.ITEM, tag);
      boolean isIn = item.is(t);
      if (!matchAll && isIn == yes) {
        return true;
      } else if (matchAll && isIn != yes) {
        return false;
      }
    }
    return matchAll;
  }

  private boolean checkPatterns(List<Pattern> patterns, boolean yes, ItemStack item, boolean exact,
                                boolean matchAll) {
    for (Pattern pattern : patterns) {
      Optional<net.minecraft.resources.ResourceKey<Item>> key = item.typeHolder().unwrapKey();
      if (key.isEmpty()) {
        continue;
      }
      Identifier id = key.get().identifier();
      String nbtStr = "";
      if (exact) {
        nbtStr = item.getComponents().toString();
      }
      boolean isIn =
          pattern.matcher(id.toString() + nbtStr).find() == yes || id.getNamespace().equals("minecraft") &&
              (pattern.matcher(id.getPath() + nbtStr).matches() == yes ||
                  pattern.matcher(id.getPath() + nbtStr.replace("minecraft:", "")).matches() == yes);
      if (!matchAll && isIn == yes) {
        return true;
      } else if (matchAll && isIn != yes) {
        return false;
      }
    }
    return matchAll;
  }
}
