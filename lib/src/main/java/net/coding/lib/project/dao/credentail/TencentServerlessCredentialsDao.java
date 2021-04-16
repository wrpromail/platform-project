package net.coding.lib.project.dao.credentail;

import net.coding.lib.project.entity.TencentServerlessCredential;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Property;

import java.sql.Timestamp;

@Mapper
public interface TencentServerlessCredentialsDao {
    int deleteByCredId(@Param("credId") Integer credId);

    TencentServerlessCredential getByConnId(
            @Param("credId") Integer credId,
            @Param("deletedAt") Timestamp deletedAt
    );

    int insert(TencentServerlessCredential record);

    int update(TencentServerlessCredential tencentServerlessCredential);

    int updateCredential(TencentServerlessCredential tencentServerlessCredential);

    int updateWasted(
            @Param("id") int id,
            @Param("wasted") boolean wasted
    );
}