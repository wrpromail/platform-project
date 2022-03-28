package net.coding.lib.project.dao.credentail;

import net.coding.lib.project.credential.entity.CredentialTask;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.sql.Timestamp;
import java.util.List;

@Mapper
public interface ProjectCredentialTaskDao {
    List<CredentialTask> getCredentialTask(
            @Param("projectId") Integer projectId,
            @Param("credId") int credId,
            @Param("type") Integer type,
            @Param("deletedAt") Timestamp deletedAt
    );

    int insert(CredentialTask record);

    int deleteByCredId(@Param("credId") Integer credId);
}