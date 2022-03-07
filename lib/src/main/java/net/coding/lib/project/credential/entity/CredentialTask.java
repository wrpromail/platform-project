package net.coding.lib.project.credential.entity;

import java.io.Serializable;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CredentialTask implements Serializable {
    private static final long serialVersionUID = 8264348081636597071L;
    private Integer id;
    private Short type;
    private Integer projectId;
    private Integer connId;
    private Integer taskId;
    private Date createdAt;
    private Date deletedAt;
}