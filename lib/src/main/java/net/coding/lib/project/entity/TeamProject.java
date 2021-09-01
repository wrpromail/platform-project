package net.coding.lib.project.entity;

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
@Table(name = "team_projects")
public class TeamProject {

    @Id
    @GeneratedValue(generator = "JDBC")
    @Column(name = "`id`", updatable = false, nullable = false)
    private Integer id;

    private Integer projectId;

    private Integer teamId;

    private Date createdAt;
    private Date updatedAt;
    private Date deletedAt;
}
