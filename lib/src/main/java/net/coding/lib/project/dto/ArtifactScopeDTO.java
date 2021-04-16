package net.coding.lib.project.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @author linyus
 * @date 2020-10-14 18:10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class ArtifactScopeDTO {
    /**
     * 制品库ID
     */
    private Integer id;
    /**
     * 制品库名称
     */
    private String name;
    /**
     * 制品库权限
     */
    private List<ProjectTokenScopeDTO> scopes;
}
