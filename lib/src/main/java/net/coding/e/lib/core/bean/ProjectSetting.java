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

    public final static String valueTrue = "1";
    public final static String valueFalse = "0";

    public final static Short open = 1;
    public final static Short close = 0;

    private static final long serialVersionUID = -1973992627485106043L;

    private Integer projectId;

    private String code;

    private String value;

    private String description;

    private Timestamp createdAt;

    private Timestamp updatedAt;

    private Timestamp deletedAt;
}
