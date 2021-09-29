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
public class ResourceSequence implements Serializable {

    private static final long serialVersionUID = 1000000000003L;

    private Integer id;

    private Integer scopeId;

    private Integer scopeType;

    private Integer code;

    private Integer oldCode;


}
