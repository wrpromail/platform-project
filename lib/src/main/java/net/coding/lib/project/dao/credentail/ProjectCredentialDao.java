package net.coding.lib.project.dao.credentail;

import com.github.pagehelper.PageRowBounds;

import net.coding.lib.project.entity.Credential;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.sql.Timestamp;
import java.util.List;

@Mapper
public interface ProjectCredentialDao {
    List<Credential> findPage(
            @Param("projectId") Integer projectId,
            @Param("userId") Integer userId,
            @Param("type") String type,
            @Param("page") PageRowBounds page,
            @Param("deletedAt") Timestamp deletedAt
    );

    Credential get(
            @Param("id") Integer id,
            @Param("projectId") Integer projectId,
            @Param("deletedAt") Timestamp deletedAt
    );

    List<Credential> getByProjectIdAndGenerateBy(
            @Param("projectId") Integer projectId,
            @Param("generateBy") String generateBy,
            @Param("deletedAt") Timestamp deletedAt
    );

    List<Credential> listByProjectAndUser(
            @Param("projectId") Integer projectId,
            @Param("userId") Integer userId,
            @Param("allSelect") boolean allSelect,
            @Param("deletedAt") Timestamp deletedAt
    );

    List<Credential> getCredential(
            @Param("projectId") Integer projectId,
            @Param("userId") Integer userId,
            @Param("id") Integer id,
            @Param("deletedAt") Timestamp deletedAt
    );

    int updateByPrimaryKeySelective(Credential credential);

    int updateBaseInfo(Credential credential);

    Credential selectByPrimaryKey(
            @Param("id") Integer id,
            @Param("deletedAt") Timestamp deletedAt
    );

    int delete(@Param("id") Integer id);

    int insertSelective(Credential credential);

    int updateSecretKey(
            @Param("id") Integer id,
            @Param("secretKey") String secretKey,
            @Param("deletedAt") Timestamp deletedAt
    );

    Credential getByCredentialId(
            @Param("credentialId") String credentialId,
            @Param("deletedAt") Timestamp deletedAt
    );


    int updateUsernamePassword(Credential credential);

    List<Credential> getByIds(
            @Param("list") List<Integer> id,
            @Param("deletedAt") Timestamp deletedAt
    );
}
