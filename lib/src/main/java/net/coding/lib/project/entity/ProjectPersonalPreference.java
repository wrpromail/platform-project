package net.coding.lib.project.entity;


import java.io.Serializable;
import java.sql.Timestamp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProjectPersonalPreference implements Serializable {
    private static final long serialVersionUID = 1L;
    private Integer id;
    private Integer projectId;
    private Integer userId;
    private String key; // 32 characters
    private String value; // 1024 characters
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private Timestamp deletedAt;

}
