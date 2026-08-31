package io.spring.core.shared;

public class Strings {
  private Strings() {}

  public static boolean isEmpty(String value) {
    return value == null || value.isEmpty();
  }
}
