package net.coding.lib.project.dto;

import net.coding.common.annotation.QiniuCDNReplace;
import net.coding.common.util.ApplicationHelper;
import net.coding.common.util.TextUtils;
import net.coding.lib.project.utils.DateUtil;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import proto.platform.user.UserProto;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {

    private static final long serialVersionUID = -7903007276342218409L;
    private String tags_str;
    private String tags;
    private String job_str;
    private int job;
    private int sex;
    private String phone;
    private String birthday;
    private String location;
    private String company;
    private String slogan;
    private String website;
    private String introduction;
    private String avatar;
    private String gravatar;
    private String lavatar;
    private long created_at;
    private long last_logined_at;
    private long last_activity_at;
    private String global_key;
    private String name;
    private String name_pinyin;
    private long updated_at;
    private String path;
    private Integer status;//not null
    private String email;
    private Integer is_member;//not null
    private Integer id;//not null
    private BigDecimal points_left;
    private String team;

    private List<String> test;

    private Long follows_count;//not null
    private Long fans_count;//not null
    private Long tweets_count;//not null
    /**
     * 电话的国家编码
     */
    private String phone_country_code;
    /**
     * 国家iso编码
     */
    private String country;
    //要使用UserDTO(User user, User cur)构造方法
    //currentUser是否已关注该用户
    private Boolean followed;//not null
    //该用户是否已关注currentUser
    private Boolean follow;//not null

    private Boolean is_phone_validated;

    private Integer email_validation;

    private Short phone_validation;

    private Integer regist_channel_id;

    private Short twofa_enabled;

    private Boolean is_welcomed;

    private String relation_with_team;

    private String qcloud_name;

    private String owner_qcloud_name;

    private String owner_uin;
    /**
     * 用户来源类型 0：coding用户；1：腾讯云用户
     */
    private Integer account_type;

    private String uin;

    //个人工作台是默认打开研发管理标签
    private Boolean is_dev;


    private String relevantURL;

    private Integer teamId;

    public UserDTO(UserProto.User user) {
        //基本类型初始化
        this.status = 0;
        this.is_member = 0;
        this.id = 0;
        this.follows_count = 0L;
        this.fans_count = 0L;
        this.tweets_count = 0L;
        this.followed = false;
        this.follow = false;
        if (user != null) {
            this.sex = user.getSex();
            this.teamId = user.getTeamId();
            this.location = TextUtils.htmlEscape(user.getLocation());
            this.company = TextUtils.htmlEscape(user.getCompany());
            this.slogan = TextUtils.htmlEscape(user.getSlogan());
            this.introduction = TextUtils.htmlEscape(user.getIntroduction());
            this.avatar = user.getAvatar();
            this.lavatar = user.getAvatar();
            this.gravatar = user.getGravatar();
            this.created_at = user.getCreatedAt();
            this.last_logined_at = user.getLastLoginedAt();
            this.global_key = TextUtils.htmlEscape(user.getGlobalKey());
            this.name = TextUtils.htmlEscape(user.getName());
            this.name_pinyin = user.getNamePinyin();
            this.updated_at = user.getUpdatedAt();
            this.status = Optional.ofNullable(user.getStatus()).orElse(1);
            this.id = Optional.ofNullable(user.getId()).orElse(0);
            this.email_validation = user.getEmailValidation();
            this.account_type = user.getAccountType();
        }
    }
}
