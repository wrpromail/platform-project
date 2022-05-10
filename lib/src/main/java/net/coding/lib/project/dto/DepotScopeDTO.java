package net.coding.lib.project.dto;

import java.util.List;

import lombok.Builder;
import lombok.Data;

/**
 * @ClassName: DepotScopeDTO
 * @description: TODO
 * @author: pual(xuyi)
 * @create: 2020-06-01 10:37
 **/
@Data
@Builder
public class DepotScopeDTO {
    private Integer id;
    private String depotName;
    private List<ProjectTokenScopeDTO> scopes;
}
