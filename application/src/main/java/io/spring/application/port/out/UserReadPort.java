package io.spring.application.port.out;

import io.spring.application.data.UserData;

public interface UserReadPort {
  UserData findByUsername(String username);

  UserData findById(String id);
}
