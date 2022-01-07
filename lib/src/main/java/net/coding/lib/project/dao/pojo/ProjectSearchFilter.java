package net.coding.lib.project.dao.pojo;

import net.coding.common.util.LimitedPager;
import net.coding.lib.project.form.ProjectFilterForm;

import java.util.Arrays;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 新版项目列表筛选器
 */
@Data
@NoArgsConstructor
public class ProjectSearchFilter extends LimitedPager {
    private ProjectFilterForm.QueryType type = ProjectFilterForm.QueryType.all;
    private String keyword;
    private Integer groupId;
    private SortBy sortBy;
    private Boolean archived = false;
    private Integer teamId;
    private Integer userId;
    private Integer lastId;

    private List<Integer> teamIds;
    private List<Integer> userIds;

    /**
     * 是否过滤掉demo项目
     */
    private boolean filterDemo = false;

    /**
     * 汇总时是否加上groupId汇总
     */
    private boolean countWithGroup = false;

    /**
     * 查看全部项目的权限
     */
    private boolean permissionProjectAll = false;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SortBy {
        private String key;
        private String value;

        // 有效的排序字段
        public static final String VISIT = "VISIT";
        public static final String CREATE = "CREATE";
        public static final String NAME = "NAME";
        public static final String OWNERID = "OWNERID";
        public static final String ARCHIVED = "ARCHIVED";


        public static final String ASC = "ASC";
        public static final String DESC = "DESC";


        private static final List<String> VALID_KEY = Arrays.asList(VISIT, CREATE, NAME, OWNERID, ARCHIVED);
        private static final List<String> VALID_VALUE = Arrays.asList(ASC, DESC);

    }
}
