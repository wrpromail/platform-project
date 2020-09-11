package net.coding.lib.project;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

/**
 * created by wang007 on 2020/7/27
 */
@ComponentScan(basePackageClasses = {
        Config.class,
})
@Import({


})
public class Config {}
