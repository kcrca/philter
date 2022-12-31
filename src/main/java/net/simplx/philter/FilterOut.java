package net.simplx.philter;

import net.minecraft.util.StringIdentifiable;

public enum FilterOut implements StringIdentifiable {
  LEFT, RIGHT, UP, DOWN;

  public String toString() {
    return name().toLowerCase();
  }

  @Override
  public String asString() {
    return toString();
  }
}
