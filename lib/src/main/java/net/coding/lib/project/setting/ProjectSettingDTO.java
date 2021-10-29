package net.coding.lib.project.setting;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder(toBuilder = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectSettingDTO {
    private Integer projectId;
    private String scope;
    private String code;
    private String value;
    private String name;
    private String description;
}
