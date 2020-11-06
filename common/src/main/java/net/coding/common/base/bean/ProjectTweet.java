package net.coding.common.base.bean;

import java.sql.Timestamp;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ProjectTweet extends BaseBean {

    private static final long serialVersionUID = 8481048593938469039L;

    public static final Short ACTION_CREATE = 1;
    public static final Short ACTION_UPDATE = 2;
    public static final Short ACTION_DELETE = 3;

    private Integer project_id;

    private Integer owner_id;

    private String content;

    private String raw;

    private Timestamp created_at;

    private Timestamp updated_at;

    private Integer comments;

    public ProjectTweet() {
        long currentTime = System.currentTimeMillis();
        this.created_at = new Timestamp(currentTime);
        this.updated_at = new Timestamp(currentTime);
    }
}
