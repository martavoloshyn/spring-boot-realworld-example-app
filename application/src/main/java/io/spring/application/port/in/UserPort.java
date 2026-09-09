package io.spring.application.port.in;

import io.spring.application.user.RegisterParam;
import io.spring.application.user.UpdateUserCommand;
import io.spring.core.user.User;
import javax.validation.Valid;

public interface UserPort {
  User createUser(@Valid RegisterParam registerParam);

  User login(String email, String password);

  void updateUser(@Valid UpdateUserCommand command);

  void follow(String username, User follower);

  void unfollow(String username, User follower);
}
