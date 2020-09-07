package net.coding.shim.project.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * created by wang007 on 2020/7/26
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HelloEvent {

    private String msg;

    private Integer id;
}
