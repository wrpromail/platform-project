package net.coding.lib.project.pager;

import net.coding.common.util.LimitedPager;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PagerResolve {

    /**
     * 需要解析的pager对象
     */
    Class<? extends net.coding.common.util.Pager> clazz() default LimitedPager.class;


}
