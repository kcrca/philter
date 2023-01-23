package net.simplx.philter;

import net.minecraft.util.StringIdentifiable;

public enum FilterMode implements StringIdentifiable {
  SAME_AS,
  MATCHES,
  NONE;

  @Override
  public String asString() {
    return toString().toLowerCase();
  }
}
