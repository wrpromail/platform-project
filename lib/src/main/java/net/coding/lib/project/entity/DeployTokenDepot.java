package net.coding.lib.project.entity;

import net.coding.common.base.bean.BaseBean;

import java.io.Serializable;
import java.sql.Timestamp;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * @ClassName: DeployTokenDepot
 * @description: TODO
 * @author: pual(xuyi)
 * @create: 2020-05-29 15:13
 **/
@Data
@Builder
public class DeployTokenDepot implements Serializable {

    private static final long serialVersionUID = 2194847159560364113L;
    private Integer id;
    private Integer deployTokenId;
    private Integer depotId;
    private String depotScope;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private Timestamp deletedAt;

}
