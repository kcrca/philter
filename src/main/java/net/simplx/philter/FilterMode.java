package net.simplx.philter;

import net.minecraft.util.StringIdentifiable;

public enum FilterMode implements StringIdentifiable {
  ONLY_SAME,
  MATCHES
  ;

  @Override
  public String asString() {
    return toString().toLowerCase();
  }
}
