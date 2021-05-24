package net.coding.lib.project.dao.credentail;

import net.coding.lib.project.entity.AndroidCredential;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.sql.Timestamp;

@Mapper
public interface AndroidCredentialDao {
    int deleteByCredId(@Param("credId") Integer credId);

    AndroidCredential getByConnId(
            @Param("credId") Integer credId,
            @Param("deletedAt") Timestamp deletedAt
    );

    int insert(AndroidCredential record);

    int updateByPrimaryKeySelective(AndroidCredential record);
}