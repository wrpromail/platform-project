package net.coding.lib.project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {

    private static final long serialVersionUID = -7903007276342218409L;
    private String phone;
    private String location;
    private String company;
    private String slogan;
    private String introduction;
    private String avatar;
    private long last_logined_at;
    private String global_key;
    private String name;
    private String name_pinyin;
    private Integer status;//not null
    private String email;
    private Integer id;//not null
    private Integer email_validation;
    private Integer phone_validation;
    private Integer account_type;//用户来源类型 0：coding用户；1：腾讯云用户
    private Integer teamId;


}
