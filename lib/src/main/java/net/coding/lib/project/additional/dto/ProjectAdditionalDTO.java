package net.coding.lib.project.additional.dto;

import net.coding.lib.project.group.ProjectGroupDTO;
import net.coding.lib.project.setting.ProjectSettingDTO;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ProjectAdditionalDTO {
    private List<ProjectSettingDTO> function;
    private List<ProjectMemberDTO> admin;
    private Long memberCount;
    private ProjectGroupDTO group;
}
