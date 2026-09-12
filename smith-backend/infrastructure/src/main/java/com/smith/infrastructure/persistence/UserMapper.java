package com.smith.infrastructure.persistence;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {

    @Select("""
            SELECT id, username, password_hash, created_at
            FROM app_user
            WHERE username = #{username}
            """)
    AppUserPo findByUsername(@Param("username") String username);
}
