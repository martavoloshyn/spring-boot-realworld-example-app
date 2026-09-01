package io.spring.graphql;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.netflix.graphql.dgs.InputArgument;
import graphql.execution.DataFetcherResult;
import io.spring.api.exception.ResourceNotFoundException;
import io.spring.application.CommentQueryService;
import io.spring.application.data.CommentData;
import io.spring.application.port.in.CommentPort;
import io.spring.core.comment.Comment;
import io.spring.core.user.User;
import io.spring.graphql.DgsConstants.MUTATION;
import io.spring.graphql.exception.AuthenticationException;
import io.spring.graphql.types.CommentPayload;
import io.spring.graphql.types.DeletionStatus;
import lombok.AllArgsConstructor;

@DgsComponent
@AllArgsConstructor
public class CommentMutation {

  private CommentPort commentPort;
  private CommentQueryService commentQueryService;

  @DgsData(parentType = MUTATION.TYPE_NAME, field = MUTATION.AddComment)
  public DataFetcherResult<CommentPayload> createComment(
      @InputArgument("slug") String slug, @InputArgument("body") String body) {
    User user = SecurityUtil.getCurrentUser().orElseThrow(AuthenticationException::new);
    Comment comment = commentPort.add(slug, body, user);
    CommentData commentData =
        commentQueryService
            .findById(comment.getId(), user)
            .orElseThrow(ResourceNotFoundException::new);
    return DataFetcherResult.<CommentPayload>newResult()
        .localContext(commentData)
        .data(CommentPayload.newBuilder().build())
        .build();
  }

  @DgsData(parentType = MUTATION.TYPE_NAME, field = MUTATION.DeleteComment)
  public DeletionStatus removeComment(
      @InputArgument("slug") String slug, @InputArgument("id") String commentId) {
    User user = SecurityUtil.getCurrentUser().orElseThrow(AuthenticationException::new);
    commentPort.delete(slug, commentId, user);
    return DeletionStatus.newBuilder().success(true).build();
  }
}
