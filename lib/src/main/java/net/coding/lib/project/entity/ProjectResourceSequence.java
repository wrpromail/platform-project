package net.coding.lib.project.entity;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProjectResourceSequence implements Serializable {
    private Integer id;

    private Integer projectId;

    private Integer code;

    private Integer oldCode;

    private static final long serialVersionUID = 1000000000003L;
}
