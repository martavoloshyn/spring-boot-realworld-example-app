package io.spring.application;

import io.spring.application.port.out.TagReadPort;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class TagsQueryService {
  private TagReadPort tagReadService;

  public List<String> allTags() {
    return tagReadService.all();
  }
}
