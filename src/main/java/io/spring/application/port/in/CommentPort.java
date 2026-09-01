package io.spring.application.port.in;

import io.spring.application.data.CommentData;
import io.spring.core.comment.Comment;
import io.spring.core.user.User;
import java.util.List;

public interface CommentPort {
  Comment add(String slug, String body, User user);

  void delete(String slug, String commentId, User user);

  List<CommentData> byArticleSlug(String slug, User user);
}
