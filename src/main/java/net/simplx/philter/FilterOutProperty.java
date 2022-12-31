package net.simplx.philter;

import java.util.Collection;
import net.minecraft.state.property.EnumProperty;

public class FilterOutProperty extends EnumProperty<FilterOut> {

  protected FilterOutProperty(String name, Collection<FilterOut> values) {
    super(name, FilterOut.class, values);
  }
}
