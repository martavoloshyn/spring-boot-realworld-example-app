package io.spring.application.article;

import io.spring.application.exception.NoAuthorizationException;
import io.spring.application.exception.ResourceNotFoundException;
import io.spring.application.port.in.ArticlePort;
import io.spring.domain.article.Article;
import io.spring.domain.article.ArticleRepository;
import io.spring.domain.favorite.ArticleFavorite;
import io.spring.domain.favorite.ArticleFavoriteRepository;
import io.spring.domain.service.AuthorizationService;
import io.spring.domain.user.User;
import javax.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@AllArgsConstructor
public class ArticleService implements ArticlePort {
  private ArticleRepository articleRepository;
  private ArticleFavoriteRepository articleFavoriteRepository;

  @Override
  public Article createArticle(@Valid NewArticleParam newArticleParam, User creator) {
    Article article =
        new Article(
            newArticleParam.getTitle(),
            newArticleParam.getDescription(),
            newArticleParam.getBody(),
            newArticleParam.getTagList(),
            creator.getId());
    articleRepository.save(article);
    return article;
  }

  @Override
  public Article update(String slug, @Valid UpdateArticleParam param, User user) {
    Article article =
        articleRepository.findBySlug(slug).orElseThrow(ResourceNotFoundException::new);
    if (!AuthorizationService.canWriteArticle(user, article)) {
      throw new NoAuthorizationException();
    }
    article.update(param.getTitle(), param.getDescription(), param.getBody());
    articleRepository.save(article);
    return article;
  }

  @Override
  public void delete(String slug, User user) {
    Article article =
        articleRepository.findBySlug(slug).orElseThrow(ResourceNotFoundException::new);
    if (!AuthorizationService.canWriteArticle(user, article)) {
      throw new NoAuthorizationException();
    }
    articleRepository.remove(article);
  }

  @Override
  public Article favorite(String slug, User user) {
    Article article =
        articleRepository.findBySlug(slug).orElseThrow(ResourceNotFoundException::new);
    articleFavoriteRepository.save(new ArticleFavorite(article.getId(), user.getId()));
    return article;
  }

  @Override
  public Article unfavorite(String slug, User user) {
    Article article =
        articleRepository.findBySlug(slug).orElseThrow(ResourceNotFoundException::new);
    articleFavoriteRepository
        .find(article.getId(), user.getId())
        .ifPresent(favorite -> articleFavoriteRepository.remove(favorite));
    return article;
  }
}
