package io.spring.infrastructure.mybatis.readservice;

import io.spring.application.data.UserData;
import io.spring.application.port.out.UserReadPort;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserReadService extends UserReadPort {

  UserData findByUsername(@Param("username") String username);

  UserData findById(@Param("id") String id);
}
