package net.coding.lib.project.parameter;

import lombok.Builder;
import lombok.Data;

/**
 * @Description: 查询项目列表请求参数
 * @Author liheng
 * @Date 2021/1/18 5:09 下午
 */
@Data
@Builder(toBuilder = true)
public class ProgramQueryParameter {

    private Integer teamId;

    private Integer userId;

    private Integer projectId;
}
