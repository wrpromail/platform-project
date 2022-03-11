package net.coding.lib.project.entity;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SearchEntity implements Serializable {

    private static final long serialVersionUID = 1604385041173L;

    /**
     * id
     */
    private Integer id;

    /**
     * 创建人编号
     */
    private Integer creatorId;

    /**
     * 项目编号
     */
    private Integer projectId;

    private String targetType;

    private Integer targetId;

    private String titleKeywords;

    private String contentKeywords;

}
