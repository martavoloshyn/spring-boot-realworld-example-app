package io.spring.api;

import io.spring.application.ArticleQueryService;
import io.spring.application.data.ArticleData;
import io.spring.application.port.in.ArticlePort;
import io.spring.core.user.User;
import java.util.HashMap;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "articles/{slug}/favorite")
@AllArgsConstructor
public class ArticleFavoriteApi {
  private ArticlePort articlePort;
  private ArticleQueryService articleQueryService;

  @PostMapping
  public ResponseEntity favoriteArticle(
      @PathVariable("slug") String slug, @AuthenticationPrincipal User user) {
    articlePort.favorite(slug, user);
    return responseArticleData(articleQueryService.findBySlug(slug, user).get());
  }

  @DeleteMapping
  public ResponseEntity unfavoriteArticle(
      @PathVariable("slug") String slug, @AuthenticationPrincipal User user) {
    articlePort.unfavorite(slug, user);
    return responseArticleData(articleQueryService.findBySlug(slug, user).get());
  }

  private ResponseEntity<HashMap<String, Object>> responseArticleData(
      final ArticleData articleData) {
    return ResponseEntity.ok(
        new HashMap<String, Object>() {
          {
            put("article", articleData);
          }
        });
  }
}
