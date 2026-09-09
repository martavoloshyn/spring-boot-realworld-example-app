package io.spring.application.user;

import io.spring.application.exception.InvalidAuthenticationException;
import io.spring.application.exception.ResourceNotFoundException;
import io.spring.application.port.in.UserPort;
import io.spring.domain.user.FollowRelation;
import io.spring.domain.user.PasswordHasher;
import io.spring.domain.user.User;
import io.spring.domain.user.UserRepository;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class UserService implements UserPort {
  private UserRepository userRepository;
  private String defaultImage;
  private PasswordHasher passwordHasher;

  @Autowired
  public UserService(
      UserRepository userRepository,
      @Value("${image.default}") String defaultImage,
      PasswordHasher passwordHasher) {
    this.userRepository = userRepository;
    this.defaultImage = defaultImage;
    this.passwordHasher = passwordHasher;
  }

  @Override
  public User createUser(@Valid RegisterParam registerParam) {
    User user =
        new User(
            registerParam.getEmail(),
            registerParam.getUsername(),
            passwordHasher.hash(registerParam.getPassword()),
            "",
            defaultImage);
    userRepository.save(user);
    return user;
  }

  @Override
  public User login(String email, String password) {
    return userRepository
        .findByEmail(email)
        .filter(user -> passwordHasher.matches(password, user.getPassword()))
        .orElseThrow(InvalidAuthenticationException::new);
  }

  @Override
  public void updateUser(@Valid UpdateUserCommand command) {
    User user = command.getTargetUser();
    UpdateUserParam updateUserParam = command.getParam();
    user.update(
        updateUserParam.getEmail(),
        updateUserParam.getUsername(),
        updateUserParam.getPassword(),
        updateUserParam.getBio(),
        updateUserParam.getImage());
    userRepository.save(user);
  }

  @Override
  public void follow(String username, User follower) {
    User target =
        userRepository.findByUsername(username).orElseThrow(ResourceNotFoundException::new);
    userRepository.saveRelation(new FollowRelation(follower.getId(), target.getId()));
  }

  @Override
  public void unfollow(String username, User follower) {
    User target =
        userRepository.findByUsername(username).orElseThrow(ResourceNotFoundException::new);
    FollowRelation relation =
        userRepository
            .findRelation(follower.getId(), target.getId())
            .orElseThrow(ResourceNotFoundException::new);
    userRepository.removeRelation(relation);
  }
}

@Constraint(validatedBy = UpdateUserValidator.class)
@Retention(RetentionPolicy.RUNTIME)
@interface UpdateUserConstraint {

  String message() default "invalid update param";

  Class[] groups() default {};

  Class[] payload() default {};
}

class UpdateUserValidator implements ConstraintValidator<UpdateUserConstraint, UpdateUserCommand> {

  @Autowired private UserRepository userRepository;

  @Override
  public boolean isValid(UpdateUserCommand value, ConstraintValidatorContext context) {
    String inputEmail = value.getParam().getEmail();
    String inputUsername = value.getParam().getUsername();
    final User targetUser = value.getTargetUser();

    boolean isEmailValid =
        userRepository.findByEmail(inputEmail).map(user -> user.equals(targetUser)).orElse(true);
    boolean isUsernameValid =
        userRepository
            .findByUsername(inputUsername)
            .map(user -> user.equals(targetUser))
            .orElse(true);
    if (isEmailValid && isUsernameValid) {
      return true;
    } else {
      context.disableDefaultConstraintViolation();
      if (!isEmailValid) {
        context
            .buildConstraintViolationWithTemplate("email already exist")
            .addPropertyNode("email")
            .addConstraintViolation();
      }
      if (!isUsernameValid) {
        context
            .buildConstraintViolationWithTemplate("username already exist")
            .addPropertyNode("username")
            .addConstraintViolation();
      }
      return false;
    }
  }
}
