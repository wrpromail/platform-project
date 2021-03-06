package net.coding.lib.project.group;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.sql.Timestamp;
import java.util.List;

@Mapper
public interface ProjectGroupDao extends tk.mybatis.mapper.common.Mapper<ProjectGroup> {
    ProjectGroup getById(@Param("id") Integer id,
                         @Param("deletedAt") Timestamp deletedAt
    );

    ProjectGroup getByUserAndName(@Param("userId") Integer userId,
                                  @Param("name") String name,
                                  @Param("deletedAt") Timestamp deletedAt
    );

    ProjectGroup getMaxSortProjectGroup(@Param("userId") Integer userId,
                                        @Param("deletedAt") Timestamp defaultDeletedAt
    );

    int countByUserIdAndType(@Param("userId") int userId,
                             @Param("type") String type,
                             @Param("deletedAt") Timestamp defaultDeletedAt
    );

    List<ProjectGroup> findAllByOwnerId(@Param("userId") Integer userId,
                                        @Param("deletedAt") Timestamp defaultDeletedAt
    );

    ProjectGroup getMinSortProjectGroup(@Param("userId") Integer userId,
                                        @Param("deletedAt") Timestamp defaultDeletedAt
    );

    List<ProjectGroup> findListAfterId(@Param("userId") Integer userId,
                                       @Param("sort") Integer sort,
                                       @Param("deletedAt") Timestamp defaultDeletedAt
    );

    void batchUpdate(List<ProjectGroup> list);

    int deleteLogical(
            @Param("id") Integer id,
            @Param("deletedAt") Timestamp defaultDeletedAt
    );

    void insertAndRetId(ProjectGroup projectGroup);
    
}

