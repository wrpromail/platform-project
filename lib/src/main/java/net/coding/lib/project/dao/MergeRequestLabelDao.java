package net.coding.lib.project.dao;

import net.coding.common.base.dao.BaseDao;
import net.coding.lib.project.dao.mapper.MergeRequestLabelMapper;
import net.coding.lib.project.entity.MergeRequestLabel;

import org.springframework.stereotype.Repository;

import java.sql.Timestamp;

import tk.mybatis.mapper.entity.Example;
import tk.mybatis.mapper.weekend.WeekendSqls;

@Repository
public class MergeRequestLabelDao {

    private final Class<?> entityClass;
    private final MergeRequestLabelMapper mapper;

    public MergeRequestLabelDao(MergeRequestLabelMapper mapper) {
        entityClass = MergeRequestLabel.class;
        this.mapper = mapper;
    }

    public long countByLabel(int labelId) {
        Example example = Example.builder(entityClass).where(
                WeekendSqls.<MergeRequestLabel>custom()
                        .andEqualTo(MergeRequestLabel::getLabelId, labelId)
                        .andEqualTo(MergeRequestLabel::getDeletedAt,
                                Timestamp.valueOf(BaseDao.NOT_DELETED))
        ).build();
        return mapper.selectCountByExample(example);
    }
}
