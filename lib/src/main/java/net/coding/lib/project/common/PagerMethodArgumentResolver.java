package net.coding.lib.project.common;

import com.github.pagehelper.PageRowBounds;

import net.coding.common.util.Pager;
import net.coding.lib.project.pager.PagerResolve;
import net.coding.lib.project.utils.HttpKit;

import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class PagerMethodArgumentResolver implements HandlerMethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(PagerResolve.class);
    }

    @Override
    public PageRowBounds resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        if (null == parameter) {
            return null;
        }
        PagerResolve annotation = parameter.getParameterAnnotation(PagerResolve.class);
        Class<? extends Pager> clazz = annotation.clazz();
        Pager pager = clazz.newInstance();
        pager.setPage(NumberUtils.toInt(HttpKit.getRequest().getParameter("page"), pager.getPage()));
        pager.setPageSize(NumberUtils.toInt(HttpKit.getRequest().getParameter("pageSize"), pager.getPageSize()));
        int offset = Math.max((pager.getPage() - 1) * pager.getPageSize(), 0);
        return new PageRowBounds(offset, pager.getPageSize());
    }


}
