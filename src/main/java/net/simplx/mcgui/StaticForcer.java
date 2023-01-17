package net.simplx.mcgui;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.simplx.mcgui.Forcer;

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

  public Object forceGet(String name) {
    return forceGet(this, name);
  }

  public Object forceGet(Object target, String name) {
    return forceGet(target, Forcer.field(clz, name));
  }

  public void forceSet(String name, Object value) {
    forceSet(Forcer.field(clz, name), value);
  }

  public void forceSet(Object target, String name, Object value) {
    forceSet(target, Forcer.field(clz, name), value);
  }
}
