package io.spring.core.user;

import io.spring.core.shared.Strings;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@EqualsAndHashCode(of = {"id"})
public class User {
  private String id;
  private String email;
  private String username;
  private String password;
  private String bio;
  private String image;

  public User(String email, String username, String password, String bio, String image) {
    this.id = UUID.randomUUID().toString();
    this.email = email;
    this.username = username;
    this.password = password;
    this.bio = bio;
    this.image = image;
  }

  public void update(String email, String username, String password, String bio, String image) {
    if (!Strings.isEmpty(email)) {
      this.email = email;
    }

    if (!Strings.isEmpty(username)) {
      this.username = username;
    }

    if (!Strings.isEmpty(password)) {
      this.password = password;
    }

    if (!Strings.isEmpty(bio)) {
      this.bio = bio;
    }

    if (!Strings.isEmpty(image)) {
      this.image = image;
    }
  }
}
