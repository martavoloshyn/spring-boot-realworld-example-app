package io.spring.application.port.out;

import io.spring.application.data.ArticleFavoriteCount;
import io.spring.domain.user.User;
import java.util.List;
import java.util.Set;

public interface ArticleFavoritesReadPort {
  boolean isUserFavorite(String userId, String articleId);

  int articleFavoriteCount(String articleId);

  List<ArticleFavoriteCount> articlesFavoriteCount(List<String> ids);

  Set<String> userFavorites(List<String> ids, User currentUser);
}
