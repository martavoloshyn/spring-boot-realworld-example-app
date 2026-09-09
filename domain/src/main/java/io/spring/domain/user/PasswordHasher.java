package io.spring.core.user;

public interface PasswordHasher {
  String hash(String rawPassword);

  boolean matches(String rawPassword, String hashedPassword);
}
