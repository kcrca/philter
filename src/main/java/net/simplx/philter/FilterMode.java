package net.simplx.philter;

import net.minecraft.util.StringIdentifiable;

public enum FilterMode implements StringIdentifiable {
  NO_NEW,
  MATCHES
  ;

  @Override
  public String asString() {
    return toString().toLowerCase();
  }
}
