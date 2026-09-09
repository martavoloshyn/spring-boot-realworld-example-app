package io.spring.api;

import io.spring.api.exception.ResourceNotFoundException;
import io.spring.application.ArticleQueryService;
import io.spring.application.article.UpdateArticleParam;
import io.spring.application.data.ArticleData;
import io.spring.application.port.in.ArticlePort;
import io.spring.core.article.Article;
import io.spring.core.user.User;
import java.util.HashMap;
import java.util.Map;
import javax.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/articles/{slug}")
@AllArgsConstructor
public class ArticleApi {
  private ArticleQueryService articleQueryService;
  private ArticlePort articlePort;

  @GetMapping
  public ResponseEntity<?> article(
      @PathVariable("slug") String slug, @AuthenticationPrincipal User user) {
    return articleQueryService
        .findBySlug(slug, user)
        .map(articleData -> ResponseEntity.ok(articleResponse(articleData)))
        .orElseThrow(ResourceNotFoundException::new);
  }

  @PutMapping
  public ResponseEntity<?> updateArticle(
      @PathVariable("slug") String slug,
      @AuthenticationPrincipal User user,
      @Valid @RequestBody UpdateArticleParam updateArticleParam) {
    Article updatedArticle = articlePort.update(slug, updateArticleParam, user);
    return ResponseEntity.ok(
        articleResponse(articleQueryService.findBySlug(updatedArticle.getSlug(), user).get()));
  }

  @DeleteMapping
  public ResponseEntity deleteArticle(
      @PathVariable("slug") String slug, @AuthenticationPrincipal User user) {
    articlePort.delete(slug, user);
    return ResponseEntity.noContent().build();
  }

  private Map<String, Object> articleResponse(ArticleData articleData) {
    return new HashMap<String, Object>() {
      {
        put("article", articleData);
      }
    };
  }
}
