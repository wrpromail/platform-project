package net.coding.lib.project.parameter;

import java.util.Set;

import lombok.Builder;
import lombok.Data;

/**
 * @Description: 查询项目列表请求参数
 * @Author liheng
 * @Date 2021/1/18 5:09 下午
 */
@Data
@Builder(toBuilder = true)
public class ProjectQueryParameter {
    private Integer userId;
    private Integer teamId;
    private String keyword;
    private Set<Integer> joinedProjectIds;
    private String label;
    private Integer invisible;
}
