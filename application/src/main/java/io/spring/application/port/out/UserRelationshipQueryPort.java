package io.spring.application.port.out;

import java.util.List;
import java.util.Set;

public interface UserRelationshipQueryPort {
  boolean isUserFollowing(String userId, String anotherUserId);

  Set<String> followingAuthors(String userId, List<String> ids);

  List<String> followedUsers(String userId);
}
