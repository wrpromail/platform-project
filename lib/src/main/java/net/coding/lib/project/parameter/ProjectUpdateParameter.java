package net.coding.lib.project.parameter;

import java.util.Date;

import lombok.Builder;
import lombok.Data;

/**
 * @Description: 更新项目请求参数
 * @Author liuying
 * @Date 2021/1/26 14:13 下午
 */
@Data
@Builder(toBuilder = true)
public class ProjectUpdateParameter {
    /**
     * 编号
     */
    private Integer id;

    /**
     * 更新时间
     */
    private Date updatedAt;

    /**
     * 名称
     */
    private String name;

    /**
     * 项目名拼音（混合版）
     */
    private String namePinyin;

    /**
     * 用于显示的名称
     */
    private String displayName;

    /**
     * 描述
     */
    private String description;

    /**
     * 项目图标
     */
    private String icon;

    /**
     * 开始日期
     */
    private Date startDate;

    /**
     * 完成日期
     */
    private Date endDate;


}
