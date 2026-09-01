package io.spring.graphql;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.netflix.graphql.dgs.InputArgument;
import io.spring.application.ProfileQueryService;
import io.spring.application.data.ProfileData;
import io.spring.application.port.in.UserPort;
import io.spring.core.user.User;
import io.spring.graphql.DgsConstants.MUTATION;
import io.spring.graphql.exception.AuthenticationException;
import io.spring.graphql.types.Profile;
import io.spring.graphql.types.ProfilePayload;
import lombok.AllArgsConstructor;

@DgsComponent
@AllArgsConstructor
public class RelationMutation {

  private UserPort userPort;
  private ProfileQueryService profileQueryService;

  @DgsData(parentType = MUTATION.TYPE_NAME, field = MUTATION.FollowUser)
  public ProfilePayload follow(@InputArgument("username") String username) {
    User user = SecurityUtil.getCurrentUser().orElseThrow(AuthenticationException::new);
    userPort.follow(username, user);
    return ProfilePayload.newBuilder().profile(buildProfile(username, user)).build();
  }

  @DgsData(parentType = MUTATION.TYPE_NAME, field = MUTATION.UnfollowUser)
  public ProfilePayload unfollow(@InputArgument("username") String username) {
    User user = SecurityUtil.getCurrentUser().orElseThrow(AuthenticationException::new);
    userPort.unfollow(username, user);
    return ProfilePayload.newBuilder().profile(buildProfile(username, user)).build();
  }

  private Profile buildProfile(@InputArgument("username") String username, User current) {
    ProfileData profileData = profileQueryService.findByUsername(username, current).get();
    return Profile.newBuilder()
        .username(profileData.getUsername())
        .bio(profileData.getBio())
        .image(profileData.getImage())
        .following(profileData.isFollowing())
        .build();
  }
}
