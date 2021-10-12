package net.coding.lib.project.parameter;

import java.util.Date;

import lombok.Builder;
import lombok.Data;

/**
 * @Author liuying
 * @Date 2021/1/26 6:13 下午
 * @Version 1.0
 */
@Data
@Builder(toBuilder = true)
public class DeployTokenUpdateParameter {
    private Integer id;

    /**
     * 权限标识
     */
    private String scope;

    /**
     * 过期时间
     */
    private Date expiredAt;

    /**
     * 权限是否应用所有仓库，0-否，1-是
     */
    private Boolean applyToAllDepots;

    /**
     * 是否将此角色下的权限应用于项目下所有制品库，0-否，1-是
     */
    private Boolean applyToAllArtifacts;


}
