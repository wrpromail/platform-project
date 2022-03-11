package net.coding.lib.project.entity;

import net.coding.common.util.BeanUtils;
import net.coding.lib.project.utils.DateUtil;

import java.io.Serializable;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NonResourceReference implements Serializable {

    private static final long serialVersionUID = 1000000000004L;

    private Integer id;

    private Integer selfId;

    private Integer selfProjectId;

    private String selfType;

    private String selfContent;

    private Integer targetId;

    private Integer targetProjectId;

    private Integer targetIid;

    private String targetType;

    private Date createdAt = DateUtil.getCurrentDate();

    private Date updatedAt = DateUtil.getCurrentDate();

    private Date deletedAt = DateUtil.strToDate(BeanUtils.NOT_DELETED_AT);
}