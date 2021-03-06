package net.coding.lib.project.group;

import net.coding.common.util.BeanUtils;
import net.coding.lib.project.utils.DateUtil;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "project_group_projects")
public class ProjectGroupProject {

    @Id
    @GeneratedValue(generator = "JDBC")
    @Column(name = "`id`", updatable = false, nullable = false)
    private Integer id;

    private Integer ownerId;

    private Integer projectGroupId;

    private Integer projectId;

    @Builder.Default
    private Date createdAt = DateUtil.getCurrentDate();
    @Builder.Default
    private Date updatedAt = DateUtil.getCurrentDate();
    @Builder.Default
    private Date deletedAt = DateUtil.strToDate(BeanUtils.NOT_DELETED_AT);

}
