package net.coding.lib.project.form;

import net.coding.common.util.LimitedPager;

import java.util.Set;

import javax.validation.constraints.NotNull;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import static java.lang.Boolean.FALSE;

@ApiModel("项目集查询表单")
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper=false)
public class QueryProgramForm extends LimitedPager {

    @ApiModelProperty(value = "查询类型", required = true)
    @NotNull(message = "param_error")
    private QueryType queryType;

    @ApiModelProperty(value = "状态")
    @Builder.Default
    private Boolean archived = FALSE;

    @ApiModelProperty(value = "负责人")
    private Set<Integer> userIds;

    @ApiModelProperty(value = "涉及项目")
    private Set<Integer> projectIds;

    @ApiModelProperty(value = "开始时间")
    private String startDate;

    @ApiModelProperty(value = "结束时间")
    private String endDate;

    @ApiModelProperty(value = "项目集关键字（名称/标识/拼音）")
    private String keyword;

    @ApiModelProperty(value = "排序类型", required = true)
    @NotNull(message = "param_error")
    private SortBy sortBy;

    public enum QueryType {
        ALL,
        JOINED,
        MANAGED
    }

    @Data
    public static class SortBy {
        private SortKey sortKey = SortKey.VISIT;
        private SortValue sortValue = SortValue.DESC;
    }

    public enum SortKey {
        VISIT,
        CREATE,
        START,
        NAME
    }

    public enum SortValue {
        ASC,
        DESC
    }

}
