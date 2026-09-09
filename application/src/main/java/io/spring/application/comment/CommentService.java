package io.spring.application.comment;

import io.spring.api.exception.NoAuthorizationException;
import io.spring.api.exception.ResourceNotFoundException;
import io.spring.application.CommentQueryService;
import io.spring.application.data.CommentData;
import io.spring.application.port.in.CommentPort;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.comment.Comment;
import io.spring.core.comment.CommentRepository;
import io.spring.core.service.AuthorizationService;
import io.spring.core.user.User;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CommentService implements CommentPort {
  private ArticleRepository articleRepository;
  private CommentRepository commentRepository;
  private CommentQueryService commentQueryService;

  @Override
  public Comment add(String slug, String body, User user) {
    Article article =
        articleRepository.findBySlug(slug).orElseThrow(ResourceNotFoundException::new);
    Comment comment = new Comment(body, user.getId(), article.getId());
    commentRepository.save(comment);
    return comment;
  }

  @Override
  public void delete(String slug, String commentId, User user) {
    Article article =
        articleRepository.findBySlug(slug).orElseThrow(ResourceNotFoundException::new);
    Comment comment =
        commentRepository
            .findById(article.getId(), commentId)
            .orElseThrow(ResourceNotFoundException::new);
    if (!AuthorizationService.canWriteComment(user, article, comment)) {
      throw new NoAuthorizationException();
    }
    commentRepository.remove(comment);
  }

  @Override
  public List<CommentData> byArticleSlug(String slug, User user) {
    Article article =
        articleRepository.findBySlug(slug).orElseThrow(ResourceNotFoundException::new);
    return commentQueryService.findByArticleId(article.getId(), user);
  }
}
