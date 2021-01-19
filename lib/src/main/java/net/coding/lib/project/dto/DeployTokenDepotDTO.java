package net.coding.lib.project.dto;

import net.coding.common.constants.DeployTokenScopeEnum;

import lombok.Builder;
import lombok.Data;

/**
 * @ClassName: DeployTokenDepotDTO
 * @description: TODO
 * @author: pual(xuyi)
 * @create: 2020-06-12 10:34
 **/
@Data
@Builder
public class DeployTokenDepotDTO {

    private String depotId;
    private String scope;

}
