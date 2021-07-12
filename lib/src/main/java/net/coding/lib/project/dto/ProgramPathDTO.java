package net.coding.lib.project.dto;

import java.io.Serializable;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder(toBuilder = true)
@ApiModel(value = "项目集创建成功响应信息")
@AllArgsConstructor
@NoArgsConstructor
public class ProgramPathDTO implements Serializable {

    @ApiModelProperty(value = "项目集Id")
    private Integer id;

    @ApiModelProperty(value = "项目集地址")
    private String path;

}
