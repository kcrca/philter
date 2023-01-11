package net.simplx.philter;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class StaticForcer implements Forcer {

  private final Class<?> clz;

  public StaticForcer(Class<?> clz) {
    this.clz = clz;
  }

  public Field field(String name) {
    return Forcer.field(clz, name);
  }

  public Method method(String name, Class<?>... parameterTypes) {
    return Forcer.method(clz, name, parameterTypes);
  }
}
