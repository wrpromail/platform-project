package net.coding.lib.project.dto;

import javax.persistence.JoinColumn;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "项目和团队成员")
public class ProjectTeamMemberDTO {
    @ApiModelProperty(value = "成员用户 ID ")
    private Integer id;

    @ApiModelProperty(value = "成员名称")
    private String name;

    @ApiModelProperty(value = "名称拼音")
    @JoinColumn(name = "name_pinyin")
    private String namePinyin;

    @ApiModelProperty(value = "头像地址")
    private String avatar;
    @ApiModelProperty(value = "是否是项目成员")
    private boolean isProjectMember;

    @ApiModelProperty(value = "最近登录时间")
    @JoinColumn(name = "last_logined_at")
    private long lastLoginedAt;
}
