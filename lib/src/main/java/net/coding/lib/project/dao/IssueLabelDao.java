package net.coding.lib.project.dao;

import java.sql.Timestamp;
import java.util.List;
import net.coding.common.base.dao.BaseDao;
import net.coding.lib.project.dao.mapper.IssueLabelMapper;
import net.coding.lib.project.entity.IssueLabel;
import org.springframework.stereotype.Repository;
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

    public int deleteByIssueId(Integer issueId) {
        Example example = Example.builder(entityClass)
                .where(WeekendSqls.<IssueLabel>custom()
                        .andEqualTo(IssueLabel::getIssueId, issueId)
                        .andEqualTo(IssueLabel::getDeletedAt,
                                Timestamp.valueOf(BaseDao.NOT_DELETED))
                )
                .build();
        return mapper.deleteByExample(example);
    }

    public List<IssueLabel> getIssueLabels(Integer issueId) {
        Example example = Example.builder(entityClass)
                .where(WeekendSqls.<IssueLabel>custom()
                        .andEqualTo(IssueLabel::getIssueId, issueId)
                        .andEqualTo(IssueLabel::getDeletedAt,
                                Timestamp.valueOf(BaseDao.NOT_DELETED))
                )
                .build();
        return mapper.selectByExample(example);
    }

    public IssueLabel getIssueLabel(Integer issueId, Integer labeldId) {
        Example example = Example.builder(entityClass)
                .where(WeekendSqls.<IssueLabel>custom()
                        .andEqualTo(IssueLabel::getIssueId, issueId)
                        .andEqualTo(IssueLabel::getLabelId, labeldId)
                        .andEqualTo(IssueLabel::getDeletedAt,
                                Timestamp.valueOf(BaseDao.NOT_DELETED))
                )
                .build();
        return mapper.selectOneByExample(example);
    }

    public List<IssueLabel> getIssueLabel(Integer issueId, List<Integer> labelIds) {
        Example example = Example.builder(entityClass)
                .where(WeekendSqls.<IssueLabel>custom()
                        .andEqualTo(IssueLabel::getIssueId, issueId)
                        .andIn(IssueLabel::getLabelId, labelIds)
                        .andEqualTo(IssueLabel::getDeletedAt,
                                Timestamp.valueOf(BaseDao.NOT_DELETED))
                )
                .build();
        return mapper.selectByExample(example);
    }

    public List<IssueLabel> getByLabels(Integer labelId) {
        Example example = Example.builder(entityClass)
                .where(WeekendSqls.<IssueLabel>custom()
                        .andEqualTo(IssueLabel::getLabelId, labelId)
                        .andEqualTo(IssueLabel::getDeletedAt,
                                Timestamp.valueOf(BaseDao.NOT_DELETED))
                )
                .build();
        return mapper.selectByExample(example);
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

    public Integer deleteIssueLables(Integer issueId, List<Integer> labelIds) {
        if (labelIds.size() == 0) {
            return 0;
        }
        Example example = Example.builder(entityClass)
                .where(WeekendSqls.<IssueLabel>custom()
                        .andEqualTo(IssueLabel::getIssueId, issueId)
                        .andIn(IssueLabel::getLabelId, labelIds)
                        .andEqualTo(IssueLabel::getDeletedAt,
                                Timestamp.valueOf(BaseDao.NOT_DELETED))
                )
                .build();
        IssueLabel label = new IssueLabel();
        label.setDeletedAt(new Timestamp(System.currentTimeMillis()));
        return mapper.updateByExample(label, example);
    }

    public IssueLabel findOrInsert(Integer issueId, Integer labelId) {
        IssueLabel issueLabel = getIssueLabel(issueId, labelId);
        if (issueLabel != null) {
            return issueLabel;
        }
        return insert(labelId, issueId);
    }

    public IssueLabel insert(Integer labelId, Integer issueId) {
        IssueLabel label = new IssueLabel();
        label.setIssueId(issueId);
        label.setLabelId(labelId);
        label.setDeletedAt(Timestamp.valueOf(BaseDao.NOT_DELETED));
        label.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        label.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        mapper.insertSelective(label);
        return label;
    }

}
