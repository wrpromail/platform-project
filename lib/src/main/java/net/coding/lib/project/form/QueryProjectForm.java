package net.coding.lib.project.form;

import net.coding.common.util.LimitedPager;

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
@EqualsAndHashCode(callSuper = false)
public class QueryProjectForm extends LimitedPager {

    @ApiModelProperty(value = "查询类型", required = true)
    @NotNull(message = "param_error")
    @Builder.Default
    private QueryType queryType = QueryType.JOINED;

    @ApiModelProperty(value = "状态")
    @Builder.Default
    private Boolean archived = FALSE;

    @ApiModelProperty(value = "项目关键字（名称/标识/拼音）")
    private String keyword;

    @ApiModelProperty(value = "分组Id")
    private Integer groupId;

    @ApiModelProperty(value = "排序类型", required = true)
    @NotNull(message = "param_error")
    private SortBy sortBy;

    public enum QueryType {
        ALL,
        JOINED,
        MANAGED,
        ARCHIVED
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
        NAME,
        ARCHIVED
    }

    public enum SortValue {
        ASC,
        DESC
    }

}
