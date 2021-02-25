package net.coding.lib.project.pager;

import com.github.pagehelper.PageRowBounds;

import net.coding.common.util.ResultPage;

import java.util.List;

public class ResultPageFactor<T> {

    /**
     * 根据 mybatis PageRowBounds 返回 ResultPage 类型
     */
    public ResultPage<T> def(PageRowBounds pager, List<T> list) {
        int page = (pager.getOffset() / pager.getLimit()) + 1;
        return new ResultPage<>(list, page, pager.getLimit(), pager.getTotal());
    }
}
