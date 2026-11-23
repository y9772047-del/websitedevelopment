package com.example.studyplan.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface RbacMapper {
    @Select("""
            SELECT r.code FROM role r
            JOIN user_role ur ON ur.role_id = r.id
            WHERE ur.user_id = #{userId}
            """)
    List<String> rolesForUser(@Param("userId") Long userId);

    @Select("""
            SELECT DISTINCT p.code FROM permission p
            JOIN role_permission rp ON rp.permission_id = p.id
            JOIN user_role ur ON ur.role_id = rp.role_id
            WHERE ur.user_id = #{userId}
            """)
    List<String> permissionsForUser(@Param("userId") Long userId);

    @Insert("""
            INSERT INTO user_role (user_id, role_id)
            SELECT #{userId}, id FROM role WHERE code = #{roleCode}
            """)
    int assignRole(@Param("userId") Long userId, @Param("roleCode") String roleCode);
}
