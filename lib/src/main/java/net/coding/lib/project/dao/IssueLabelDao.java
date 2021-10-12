package net.coding.lib.project.dao;

import net.coding.common.base.dao.BaseDao;
import net.coding.lib.project.dao.mapper.IssueLabelMapper;
import net.coding.lib.project.entity.IssueLabel;

import org.springframework.stereotype.Repository;

import java.sql.Timestamp;

import tk.mybatis.mapper.entity.Example;
import tk.mybatis.mapper.weekend.WeekendSqls;

@Repository
public class IssueLabelDao {

    private Class<?> entityClass;
    private IssueLabelMapper mapper;

    public IssueLabelDao(IssueLabelMapper mapper) {
        entityClass = IssueLabel.class;
        this.mapper = mapper;
    }

    public int deleteByLabelId(Integer labelId) {
        Example example = Example.builder(entityClass)
                .where(WeekendSqls.<IssueLabel>custom()
                        .andEqualTo(IssueLabel::getLabelId, labelId)
                        .andEqualTo(IssueLabel::getDeletedAt,
                                Timestamp.valueOf(BaseDao.NOT_DELETED))
                )
                .build();
        IssueLabel label = new IssueLabel();
        label.setDeletedAt(new Timestamp(System.currentTimeMillis()));
        return mapper.updateByExampleSelective(label, example);
    }

}
