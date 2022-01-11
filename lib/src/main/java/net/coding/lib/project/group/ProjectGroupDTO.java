package net.coding.lib.project.group;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class ProjectGroupDTO {
    private Integer ownerId;
    private String name;
    private Integer sort;
    private Integer id;
    private Long projectNum;
    private String type;
}
