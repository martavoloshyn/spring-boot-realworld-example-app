package io.spring.application.port.in;

import io.spring.application.article.NewArticleParam;
import io.spring.application.article.UpdateArticleParam;
import io.spring.core.article.Article;
import io.spring.core.user.User;
import javax.validation.Valid;

public interface ArticlePort {
  Article createArticle(@Valid NewArticleParam newArticleParam, User creator);

  Article update(String slug, @Valid UpdateArticleParam param, User user);

  void delete(String slug, User user);

  Article favorite(String slug, User user);

  Article unfavorite(String slug, User user);
}
