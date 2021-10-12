package net.coding.lib.project.entity;

import java.io.Serializable;
import java.sql.Timestamp;

import lombok.Builder;
import lombok.Data;

/**
 * @ClassName: ProjectTokenDepot
 * @description: TODO
 * @author: pual(xuyi)
 * @create: 2020-05-29 15:13
 **/
@Data
@Builder
public class ProjectTokenDepot implements Serializable {

    private static final long serialVersionUID = 2194847159560364113L;
    private Integer id;
    private Integer deployTokenId;
    private Integer depotId;
    private String depotScope;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private Timestamp deletedAt;

}
