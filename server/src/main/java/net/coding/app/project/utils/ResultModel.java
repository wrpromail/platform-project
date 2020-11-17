package net.coding.app.project.utils;

public class ResultModel<D> {

    private String code;
    private D data;
    private Integer currentTime;
    private String msg;

    public ResultModel() {

    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public D getData() {
        return data;
    }

    public void setData(D data) {
        this.data = data;
    }

    public Integer getCurrentTime() {
        return currentTime;
    }

    public void setCurrentTime(Integer currentTime) {
        this.currentTime = currentTime;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public static <T> ResultModel<T> getInstance(String code, T data) {
        return getInstance(code, (String)null, (int)(System.currentTimeMillis() / 1000L), data);
    }

    public static <T> ResultModel<T> getInstance(String code, Integer currentTime, T data) {
        return getInstance(code, (String)null, currentTime, data);
    }

    public static <T> ResultModel<T> getInstance(String code, String msg, Integer currentTime, T data) {
        ResultModel<T> rm = new ResultModel();
        rm.setCode(code);
        rm.setCurrentTime(currentTime);
        rm.setData(data);
        rm.setMsg(msg);
        return rm;
    }

    @Override
    public String toString() {
        return "ResultModel{code=" + this.code + ", data=" + this.data + ", currentTime=" + this.currentTime + ", msg='" + this.msg + '\'' + '}';
    }
}
