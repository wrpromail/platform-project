package net.coding.lib.project.exception;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class ExceptionMessage {
    @Builder.Default
    private String code = "-1";
    private Map<String, String> msg;
    private Map<String, String> data;

    public static ExceptionMessage of(String key, String message) {
        return ExceptionMessage.builder()
                .msg(ImmutableMap.of(key, message))
                .build();
    }

    public static ExceptionMessage of(String code, String key, String message) {
        return ExceptionMessage.builder()
                .code(code)
                .msg(ImmutableMap.of(key, message))
                .build();
    }

    public static ExceptionMessage of(String code, String key, String message, Map<String, String> data) {
        return ExceptionMessage.builder()
                .code(code)
                .msg(ImmutableMap.of(key, message))
                .data(data)
                .build();
    }
}