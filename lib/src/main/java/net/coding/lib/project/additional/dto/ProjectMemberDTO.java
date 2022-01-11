package net.coding.lib.project.additional.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class ProjectMemberDTO {
    private String avatar;
    private String name;
    private String namePinyin;
}
