package net.coding.lib.project.form;

import java.util.Set;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@ApiModel("项目集添加项目/成员表单")
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateProgramAddProjectForm {

    @ApiModelProperty(value = "协作项目")
    private Set<Integer> projectIds;

    @ApiModelProperty(value = "管理员")
    private Set<Integer> userIds;

}
