package net.simplx.philter;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public interface Forcer {

  private static <T extends AccessibleObject> Optional<T> walkSupertypes(Class<?> clz,
      Function<Class<?>, Optional<T>> lookup) {
    Optional<T> optMember = lookup.apply(clz);
    if (optMember.isPresent()) {
      optMember.get().setAccessible(true);
      return optMember;
    }

    List<Class<?>> supertypes = new ArrayList<>(List.of(clz.getInterfaces()));
    if (clz.getSuperclass() != null) {
      supertypes.add(clz.getSuperclass());
    }
    for (var stype : supertypes) {
      optMember = walkSupertypes(stype, lookup);
      if (optMember.isPresent()) {
        return optMember;
      }
    }
    return Optional.empty();
  }

  static Field field(Class<?> clz, String name) {
    var optMember = walkSupertypes(clz, stype -> {
      try {
        return Optional.of(stype.getDeclaredField(name));
      } catch (NoSuchFieldException e) {
        return Optional.empty();
      }
    });
    if (optMember.isEmpty()) {
      throw new IllegalStateException(name + ": Field not found");
    }
    return optMember.get();
  }

  static Method method(Class<?> clz, String name, Class<?>... parameterTypes) {
    var optMember = walkSupertypes(clz, stype -> {
      try {
        return Optional.of(stype.getDeclaredMethod(name, parameterTypes));
      } catch (NoSuchMethodException e) {
        return Optional.empty();
      }
    });
    if (optMember.isEmpty()) {
      throw new IllegalStateException(name + ": Method not found");
    }
    return optMember.get();
  }

  default Object forceGet(Field field) {
    return forceGet(this, field);
  }

  default Object forceGet(Object obj, Field field) {
    try {
      return field.get(obj);
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
    return forceInvoke(this, method, parameters);
  }

  default Object forceInvoke(Object obj, Method method, Object... parameters) {
    try {
      return method.invoke(obj, parameters);
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new IllegalStateException(e);
    }
  }
}
