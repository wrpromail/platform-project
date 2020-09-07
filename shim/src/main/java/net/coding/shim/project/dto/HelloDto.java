package net.coding.shim.project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 这个 DTO， 在 client 和 lib 模块下都有使用。
 * <p>
 * created by wang007 on 2020/7/26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HelloDto {

    private Integer id;
    private String msg;
}
