package net.coding.lib.project.entity;

import net.coding.common.base.bean.BaseBean;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * created by wang007 on 2020/7/25
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Hello extends BaseBean {

    private String msg;
}
