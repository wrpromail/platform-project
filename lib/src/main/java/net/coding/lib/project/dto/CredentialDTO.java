package net.coding.lib.project.dto;


import java.util.ArrayList;
import java.util.List;

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
@ApiModel(value = "项目凭据")
public class CredentialDTO {

    @ApiModelProperty(value = "主键 id")
    private Integer id;
    @ApiModelProperty(value = "项目 id")
    private Integer projectId;
    @ApiModelProperty(value = "凭据串")
    private String credentialId;
    @ApiModelProperty(value = "类型")
    private String type;
    @ApiModelProperty(value = "scope")
    private Integer scope;
    @ApiModelProperty(value = "名称")
    private String name;
    @ApiModelProperty(value = "用户名称")
    private String username;
    @ApiModelProperty(value = "jenkins 中的凭据类型")
    private Integer scheme;
    @ApiModelProperty(value = "地址")
    private String url;
    @ApiModelProperty(value = "Kubeconfig,ServiceAccount")
    private String verificationMethod;
    @ApiModelProperty(value = "kubConfig")
    private String kubConfig;
    @ApiModelProperty(value = "k8s 集群名称")
    private String clusterName;
    @ApiModelProperty(value = "是否接受不信任证书")
    private Boolean acceptUntrustedCertificates;
    @ApiModelProperty(value = "连通状态")
    private Integer state;
    @ApiModelProperty(value = "头像")
    private String avatar;
    @ApiModelProperty(value = "昵称")
    private String nickname;
    @ApiModelProperty(value = "token")
    private String token;
    @ApiModelProperty(value = "应用 id")
    private String appId;
    @ApiModelProperty(value = "secretId")
    private String secretId;
    @ApiModelProperty(value = "secretKey")
    private String secretKey;
    @ApiModelProperty(value = "描述")
    private String description;
    @ApiModelProperty(value = "创建时间")
    private long created_at;
    @ApiModelProperty(value = "更新时间")
    private long updated_at;
    @ApiModelProperty(value = "任务数量")
    private Integer taskCount;
    @ApiModelProperty(value = "关联任务")
    @Builder.Default
    private List<ConnectionTaskDTO> selectedTasks = new ArrayList<>();
    @ApiModelProperty(value = "是否全部选择")
    @Builder.Default
    private boolean allSelect = false;
    @ApiModelProperty(value = "是否失效")
    @Builder.Default
    private boolean failure = false;
    // Android
    @ApiModelProperty(value = "文件名称")
    private String fileName;
    @ApiModelProperty(value = "别名")
    private String alias;
    @ApiModelProperty(value = "sha1")
    private String sha1;
}
