package io.spring.api;

import static org.springframework.web.bind.annotation.RequestMethod.POST;

import com.fasterxml.jackson.annotation.JsonRootName;
import io.spring.application.UserQueryService;
import io.spring.application.data.UserData;
import io.spring.application.data.UserWithToken;
import io.spring.application.port.in.UserPort;
import io.spring.application.user.RegisterParam;
import io.spring.core.service.JwtService;
import io.spring.core.user.User;
import java.util.HashMap;
import java.util.Map;
import javax.validation.Valid;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class UsersApi {
  private UserPort userPort;
  private UserQueryService userQueryService;
  private JwtService jwtService;

  @RequestMapping(path = "/users", method = POST)
  public ResponseEntity createUser(@Valid @RequestBody RegisterParam registerParam) {
    User user = userPort.createUser(registerParam);
    UserData userData = userQueryService.findById(user.getId()).get();
    return ResponseEntity.status(201)
        .body(userResponse(new UserWithToken(userData, jwtService.toToken(user))));
  }

  @RequestMapping(path = "/users/login", method = POST)
  public ResponseEntity userLogin(@Valid @RequestBody LoginParam loginParam) {
    User user = userPort.login(loginParam.getEmail(), loginParam.getPassword());
    UserData userData = userQueryService.findById(user.getId()).get();
    return ResponseEntity.ok(userResponse(new UserWithToken(userData, jwtService.toToken(user))));
  }

  private Map<String, Object> userResponse(UserWithToken userWithToken) {
    return new HashMap<String, Object>() {
      {
        put("user", userWithToken);
      }
    };
  }
}

@Getter
@JsonRootName("user")
@NoArgsConstructor
class LoginParam {
  @NotBlank(message = "can't be empty")
  @Email(message = "should be an email")
  private String email;

  @NotBlank(message = "can't be empty")
  private String password;
}
