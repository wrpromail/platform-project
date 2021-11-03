package net.coding.e.lib.core.bean;

import net.coding.common.base.bean.BaseBean;

import java.sql.Timestamp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProjectSetting extends BaseBean {

    private static final long serialVersionUID = -1973992627485106043L;

    private Integer projectId;
    private String code;
    private String value;
    private String description;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private Timestamp deletedAt;
}
