package net.coding.common.base.bean;

import net.coding.common.base.bean.BaseBean;

import java.sql.Timestamp;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class Notification extends BaseBean {
    private static final long serialVersionUID = -6758768212375001881L;

    public static final Short STATUS_UNREAD = 0;
    public static final Short STATUS_READ = 1;

    public static final Short TYPE_AT = 0;
    public static final Short TYPE_REPLY = 1;
    public static final Short TYPE_LIKE = 2;
    public static final Short TYPE_SUBSCRIBE = 3;
    public static final Short TYPE_SYSTEM = 4;
    public static final Short TYPE_OAUTH = 5;
    public static final Short TYPE_TWEET = 6;
    public static final Short TYPE_CI = 7;
    public static final Short TYPE_QFLOW = 8;
    public static final Short TYPE_CD = 9;

    private Integer owner_id;

    private Timestamp created_at;

    private Short status;

    private Short type;

    private String target_type;

    private String target_id;

    private String content;

    public Notification() {
        created_at = new Timestamp(System.currentTimeMillis());
    }
}
