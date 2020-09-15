package net.coding.app.project.utils;

import java.util.HashMap;
import java.util.List;

public class ResponseUtil {
    /**
     * 构建默认的成功响应信息
     *
     * @param
     * @return
     */
    public static ResultModel buildSuccessResponse() {
        ResultModel model = ResultModel.getInstance("0", "请求成功", (int) (System.currentTimeMillis() / 1000L), null);
        return model;
    }

    /**
     * 构建默认的成功响应信息
     *
     * @param obj
     * @return
     */
    public static ResultModel buildSuccessResponse(Object obj) {
        return ResultModel.getInstance("0", "请求成功", (int) (System.currentTimeMillis() / 1000L), obj);
    }

    /**
     * 构建默认的成功响应信息
     *
     * @param
     * @return
     */
    public static ResultModel buildSuccessResponseByList(List list) {
        return ResultModel.getInstance("0", "请求成功", (int) (System.currentTimeMillis() / 1000L), list);
    }

    /**
     * 构建成功响应信息
     *
     * @param obj
     * @param tip
     * @return
     */
    public static ResultModel buildSuccessResponse(Object obj, String tip) {
        return ResultModel.getInstance("0", "请求成功", (int) (System.currentTimeMillis() / 1000L), obj);
    }

    /**
     * 构建失败响应信息
     *
     * @param code
     * @return
     */
    public static ResultModel buildFaildResponse(String code) {
        return ResultModel.getInstance(code, "请求失败", (int) (System.currentTimeMillis() / 1000L), new HashMap<>());
    }

    /**
     * 构建失败响应信息并返回相关数据
     *
     * @param code
     * @return
     */
    public static ResultModel buildFaildResponse(String code, String msg) {
        return ResultModel.getInstance(code, msg, (int) (System.currentTimeMillis() / 1000L), null);
    }

    /**
     * 构建失败响应信息并返回相关数据
     *
     * @param code
     * @return
     */
    public static ResultModel buildFaildResponse(String code, Object obj) {
        return ResultModel.getInstance(code, "请求失败", (int) (System.currentTimeMillis() / 1000L), obj);
    }
}
