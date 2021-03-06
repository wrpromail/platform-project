package net.coding.lib.project.dao;

import net.coding.common.base.dao.BaseDao;
import net.coding.lib.project.dao.mapper.ProjectLabelMapper;
import net.coding.lib.project.entity.ProjectLabel;

import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

import tk.mybatis.mapper.entity.Example;
import tk.mybatis.mapper.weekend.WeekendSqls;


@Repository
public class ProjectLabelDao {

    private final Class<?> entityClass;
    private final ProjectLabelMapper mapper;

    public ProjectLabelDao(ProjectLabelMapper mapper) {
        entityClass = ProjectLabel.class;
        this.mapper = mapper;
    }

    public ProjectLabel getByNameAndProject(String name, int projectId) {
        Example example = Example.builder(entityClass).where(
                WeekendSqls.<ProjectLabel>custom()
                        .andEqualTo(ProjectLabel::getName, name)
                        .andEqualTo(ProjectLabel::getProjectId, projectId)
                        .andEqualTo(ProjectLabel::getDeletedAt,
                                Timestamp.valueOf(BaseDao.NOT_DELETED))
        ).build();
        return mapper.selectByExample(example).stream().findFirst().orElse(null);
    }

    public List<ProjectLabel> findByProjectId(int projectId) {
        Example example = Example.builder(entityClass).where(
                WeekendSqls.<ProjectLabel>custom()
                        .andEqualTo(ProjectLabel::getProjectId, projectId)
                        .andEqualTo(ProjectLabel::getDeletedAt,
                                Timestamp.valueOf(BaseDao.NOT_DELETED))
        ).orderByAsc("name").build();
        return mapper.selectByExample(example);
    }

    public ProjectLabel findById(int id) {
        Example example = Example.builder(entityClass).where(
                WeekendSqls.<ProjectLabel>custom()
                        .andEqualTo(ProjectLabel::getId, id)
                        .andEqualTo(ProjectLabel::getDeletedAt,
                                Timestamp.valueOf(BaseDao.NOT_DELETED))
        ).build();
        return mapper.selectOneByExample(example);
    }

    public ProjectLabel findByIdWitDeleted(int id) {
        ProjectLabel label = new ProjectLabel();
        label.setId(id);
        return mapper.selectByPrimaryKey(label);
    }

    public int insert(ProjectLabel label) {
        label.setDeletedAt(Timestamp.valueOf(BaseDao.NOT_DELETED));
        label.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        label.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        if (0 < mapper.insertSelective(label)) {
            return label.getId();
        }
        return 0;
    }

    public int update(ProjectLabel label) {
        label.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        return mapper.updateByPrimaryKeySelective(label);
    }

    public int delete(ProjectLabel label) {
        label.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        label.setDeletedAt(new Timestamp(System.currentTimeMillis()));
        return mapper.updateByPrimaryKeySelective(label);
    }

    public List<ProjectLabel> findByIds(List<Integer> ids) {
        Example example = Example.builder(entityClass).where(
                WeekendSqls.<ProjectLabel>custom()
                        .andIn(ProjectLabel::getId, ids)
                        .andEqualTo(ProjectLabel::getDeletedAt,
                                Timestamp.valueOf(BaseDao.NOT_DELETED))
        ).build();
        return mapper.selectByExample(example);
    }

    public List<ProjectLabel> findByIdsWithDeleted(List<Integer> ids) {
        Example example = Example.builder(entityClass).where(
                WeekendSqls.<ProjectLabel>custom()
                        .andIn(ProjectLabel::getId, ids)
        ).build();
        return mapper.selectByExample(example);
    }

    public List<ProjectLabel> getLabelsByProjectIdAndNames(List<String> names, List<Integer> projectIdList) {
        Example example = Example.builder(entityClass).where(
                WeekendSqls.<ProjectLabel>custom()
                        .andIn(ProjectLabel::getProjectId, projectIdList)
                        .andIn(ProjectLabel::getName, names)
                        .andEqualTo(ProjectLabel::getDeletedAt,
                                Timestamp.valueOf(BaseDao.NOT_DELETED))
        ).build();
        return mapper.selectByExample(example);
    }

    public List<ProjectLabel> getLabelsByProjects(List<Integer> projectIdList){
        Example example = Example.builder(entityClass).where(
                WeekendSqls.<ProjectLabel>custom()
                        .andIn(ProjectLabel::getProjectId, projectIdList)
                        .andEqualTo(ProjectLabel::getDeletedAt,
                                Timestamp.valueOf(BaseDao.NOT_DELETED))
        ).build();
        return mapper.selectByExample(example);
    }
}
