package net.coding.lib.project.dto;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DeployTokenDepotDTO {

    private String depotId;
    private String scope;

}
