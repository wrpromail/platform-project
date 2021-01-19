package net.coding.app.project.advice;

import net.coding.common.i18n.utils.LocaleMessageSourceUtil;
import net.coding.lib.project.exception.AppException;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.exception.ExceptionMessage;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Optional;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ControllerAdvice
@AllArgsConstructor
public class ExceptionAdvice {

    private final static String NETWORK_CONNECTION_ERROR = "network_connection_error";
    private final static String NETWORK_CONNECTION_ERROR_CODE = "905";

    /**
     * validate验证异常
     */
    @ExceptionHandler({BindException.class, MethodArgumentNotValidException.class})
    @ResponseBody
    public ResponseEntity<ExceptionMessage> bindExceptionHandler(Exception ex) {
        if (log.isDebugEnabled()) {
            log.error("Exception advice bind exception", ex);
        } else {
            log.warn("Exception advice bind exception {}", ex.getMessage());
        }
        BindingResult bindingResult = null;
        if (ex instanceof BindException) {
            BindException e = (BindException) ex;
            bindingResult = e.getBindingResult();
        } else if (ex instanceof MethodArgumentNotValidException) {
            MethodArgumentNotValidException e = (MethodArgumentNotValidException) ex;
            bindingResult = e.getBindingResult();
        }
        if (null == bindingResult) {
            return ResponseEntity.ok().body(defaultMessage());
        }
        ObjectError first = bindingResult
                .getAllErrors()
                .stream()
                .findFirst()
                .orElse(null);
        if (first == null) {
            return ResponseEntity.ok().body(defaultMessage());
        }
        String message = LocaleMessageSourceUtil.getMessage(first.getDefaultMessage(),
                first.getArguments(),
                NETWORK_CONNECTION_ERROR
        );
        if (message == null) {
            return ResponseEntity.ok().body(defaultMessage());
        }
        return ResponseEntity.ok().body(
                ExceptionMessage.of(
                        first.getDefaultMessage(),
                        message
                )
        );
    }


    @ExceptionHandler(AppException.class)
    @ResponseBody
    public ResponseEntity<ExceptionMessage> handleError(AppException exception) {
        if (log.isDebugEnabled()) {
            log.error("Exception advice handle app exception", exception);
        } else {
            log.warn("Exception advice handle app exception, code = {}, msg = {}", exception.getCode(), exception.getKey());
        }
        String message = LocaleMessageSourceUtil.getMessage(
                exception.getKey(),
                Optional.ofNullable(exception.getArguments()).orElse(new Object[0]),
                NETWORK_CONNECTION_ERROR
        );
        if (message == null) {
            return ResponseEntity.ok().body(defaultMessage());
        }
        return ResponseEntity.ok().body(
                ExceptionMessage.of(
                        String.valueOf(exception.getCode()),
                        exception.getKey(),
                        message
                )
        );
    }

    @ExceptionHandler(CoreException.class)
    @ResponseBody
    public ResponseEntity<ExceptionMessage> handleError(CoreException exception) {
        if (log.isDebugEnabled()) {
            log.error("Exception advice handle app exception", exception);
        } else {
            log.warn("Exception advice handle app exception, code = {}, msg = {}", exception.getCode(), exception.getKey());
        }
        String message = exception.getMsg();
        if (message == null) {
            return ResponseEntity.ok().body(defaultMessage());
        }
        return ResponseEntity.ok().body(
                ExceptionMessage.of(
                        String.valueOf(exception.getCode()),
                        exception.getKey(),
                        message
                )
        );
    }


    @ExceptionHandler
    @ResponseBody
    public ResponseEntity<ExceptionMessage> handle(Exception exception) {
        log.error("Exception advice handle exception", exception);
        return ResponseEntity.ok().body(defaultMessage());
    }


    private ExceptionMessage defaultMessage() {
        String message = LocaleMessageSourceUtil.getMessage(
                NETWORK_CONNECTION_ERROR,
                new Object[]{},
                NETWORK_CONNECTION_ERROR
        );
        return ExceptionMessage.of(
                NETWORK_CONNECTION_ERROR_CODE,
                NETWORK_CONNECTION_ERROR,
                message
        );
    }
}
