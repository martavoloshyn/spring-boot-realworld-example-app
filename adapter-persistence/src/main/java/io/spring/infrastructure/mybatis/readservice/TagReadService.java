package io.spring.infrastructure.mybatis.readservice;

import io.spring.application.port.out.TagReadPort;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TagReadService extends TagReadPort {
  List<String> all();
}
