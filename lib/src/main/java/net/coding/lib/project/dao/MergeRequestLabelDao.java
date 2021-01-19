package net.coding.lib.project.dao;

import java.sql.Timestamp;
import java.util.List;
import lombok.AllArgsConstructor;
import net.coding.common.base.dao.BaseDao;
import net.coding.lib.project.dao.mapper.MergeRequestLabelMapper;
import net.coding.lib.project.entity.MergeRequestLabel;
import org.springframework.stereotype.Repository;
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

    public MergeRequestLabel find(int mergeRequestId, int labelId) {

        Example example = Example.builder(entityClass).where(
                WeekendSqls.<MergeRequestLabel>custom()
                        .andEqualTo(MergeRequestLabel::getMergeRequestId, mergeRequestId)
                        .andEqualTo(MergeRequestLabel::getLabelId, labelId)
                        .andEqualTo(MergeRequestLabel::getDeletedAt,
                                Timestamp.valueOf(BaseDao.NOT_DELETED))
        ).build();
        return mapper.selectOneByExample(example);
    }

    public boolean exists(int mergeRequestId, int labelId) {
        Example example = Example.builder(entityClass).where(
                WeekendSqls.<MergeRequestLabel>custom()
                        .andEqualTo(MergeRequestLabel::getMergeRequestId, mergeRequestId)
                        .andEqualTo(MergeRequestLabel::getLabelId, labelId)
                        .andEqualTo(MergeRequestLabel::getDeletedAt,
                                Timestamp.valueOf(BaseDao.NOT_DELETED))
        ).build();
        return mapper.selectCountByExample(example) > 0;
    }

    public List<MergeRequestLabel> list(int mergeRequestId) {
        Example example = Example.builder(entityClass).where(
                WeekendSqls.<MergeRequestLabel>custom()
                        .andEqualTo(MergeRequestLabel::getMergeRequestId, mergeRequestId)
                        .andEqualTo(MergeRequestLabel::getDeletedAt,
                                Timestamp.valueOf(BaseDao.NOT_DELETED))
        ).build();
        return mapper.selectByExample(example);
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

    public int delete(int mergeRequestId) {
        Example example = Example.builder(entityClass).where(
                WeekendSqls.<MergeRequestLabel>custom()
                        .andEqualTo(MergeRequestLabel::getMergeRequestId, mergeRequestId)
                        .andEqualTo(
                                MergeRequestLabel::getDeletedAt,
                                Timestamp.valueOf(BaseDao.NOT_DELETED)
                        )
        ).build();
        MergeRequestLabel label = MergeRequestLabel.builder()
                .deletedAt(new Timestamp(System.currentTimeMillis()))
                .build();
        return mapper.updateByExampleSelective(label, example);
    }

    public MergeRequestLabel insert(int mergeRequestId, int labelId) {
        MergeRequestLabel label = MergeRequestLabel.builder()
                .mergeRequestId(mergeRequestId)
                .labelId(labelId)
                .updatedAt(new Timestamp(System.currentTimeMillis()))
                .deletedAt(Timestamp.valueOf(BaseDao.NOT_DELETED))
                .createdAt(new Timestamp(System.currentTimeMillis()))
                .build();
        if (mapper.insertSelective(label) > 0) {
            return label;
        }
        return null;
    }
}
