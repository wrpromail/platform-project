package net.coding.lib.project.dao;

import net.coding.lib.project.entity.UserProjectSetting;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.sql.Timestamp;
import java.util.List;

@Mapper
public interface UserProjectSettingDao {
    List<UserProjectSetting> getUserProjectSettingsByCodes(
            @Param("list") List<String> code,
            @Param("projectId") int projectId,
            @Param("userId") int userId,
            @Param("deletedAt") Timestamp deletedAt
    );

    UserProjectSetting getUserProjectSetting(
            @Param("code") String code,
            @Param("projectId") int projectId,
            @Param("userId") int userId,
            @Param("deletedAt") Timestamp deletedAt
    );

    void updateValue(@Param("id") int id, @Param("value") String value);

    int insert(UserProjectSetting userProjectSetting);
}
