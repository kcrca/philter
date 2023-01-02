package net.simplx.philter;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public interface Forcer {

  static Field field(Class<?> clz, String name) {
    try {
      var field = clz.getDeclaredField(name);
      field.setAccessible(true);
      return field;
    } catch (NoSuchFieldException e) {
      throw new IllegalStateException(e);
    }
  }

  static Method method(Class<?> clz, String name, Class<?>... parameterTypes) {
    try {
      var method = clz.getDeclaredMethod(name, parameterTypes);
      method.setAccessible(true);
      return method;
    } catch (NoSuchMethodException e) {
      throw new IllegalStateException(e);
    }
  }

  default Object forceGet(Field field) {
    try {
      return field.get(this);
    } catch (IllegalAccessException e) {
      throw new IllegalStateException(e);
    }
  }

  default void forceSet(Field field, Object value) {
    try {
      field.set(this, value);
    } catch (IllegalAccessException e) {
      throw new IllegalStateException(e);
    }
  }

  default Object forceInvoke(Method method, Object... parameters) {
    try {
      return method.invoke(this, parameters);
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new IllegalStateException(e);
    }
  }
}
