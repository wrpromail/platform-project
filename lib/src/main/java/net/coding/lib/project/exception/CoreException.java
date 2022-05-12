package net.coding.lib.project.exception;

import net.coding.common.base.validator.ValidationConstants;
import net.coding.common.constants.CommonConstants;
import net.coding.common.db.api.DBException;
import net.coding.common.util.AllowOriginUtil;
import net.coding.common.util.QcloudUtils;
import net.coding.common.util.Result;
import net.coding.lib.project.utils.ResourceUtil;

import org.springframework.beans.InvalidPropertyException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

import static net.coding.common.base.validator.ValidationConstants.PROJECT_DIR_NAME_LENGTH;
import static net.coding.common.base.validator.ValidationConstants.PROJECT_DISPLAY_NAME_MAX_LENGTH;
import static net.coding.common.base.validator.ValidationConstants.PROJECT_DISPLAY_NAME_MIN_LENGTH;
import static net.coding.common.base.validator.ValidationConstants.PROJECT_FILE_NAME_LENGTH;
import static net.coding.common.base.validator.ValidationConstants.PROJECT_NAME_MAX_LENGTH;
import static net.coding.common.base.validator.ValidationConstants.PROJECT_NAME_MIN_LENGTH;

@ControllerAdvice
@Slf4j
public class CoreException extends Exception {
    public enum ExceptionType {
        USER_NOT_LOGIN(1000, "user_not_login"),
        DEFAULT_FAIL_EXCEPTION(1, "default_fail_exception"),

        // common
        PARAMETER_INVALID(900, "param_error"),
        RESOURCE_NO_FOUND(404, "resource_not_found"),
        CONTENT_INCLUDE_SENSITIVE_WORDS(901, "content_include_sensitive_words"),
        IMAGE_BIG_THAN_5M(902, "image_big_than_5m"),
        CAPTCHA_ERROR(903, "j_captcha_error"),
        OPERATING_FREQUENCY_FAST(904, "operating_frequency_fast"),
        NETWORK_CONNECTION_ERROR(905, "network_connection_error"),
        CONTENT_EQUALS_RESERVED_WORDS(906, "content_equals_reserved_words"),
        OPERATION_NEED_CAPTCHA(907, "operation_need_captcha"),
        GLOBAL_KEY_INVALID(908, "global_key_invalid"),
        CAPTCHA_NOT_EMPTY(909, "j_captcha_not_empty"),
        DATE_FORMAT_INVALID(910, "date_format_invalid"),
        DATE_START_FORMAT_INVALID(911, "date_start_format_invalid"),
        DATE_END_FORMAT_INVALID(912, "date_end_format_invalid"),
        IMPORT_PROJECT_FAILED(913, "import_project_failed"),
        METHOD_NOW_ALLOWED(914, "method_not_allowed"),
        TEAM_CAN_NOT_CLEAR_NORMAL_USER(914, "team_can_not_clear_normal_user"),
        SIGN_ERROR(915, "sign_error"),
        ORIGIN_ERROR(916, "origin_error"),
        DATE_ERROR(917, "date_error"),
        COUNT_LIMIT_ERROR(918, "count_limit_error"),
        CAPTCHA_SLIDE_ERROR(919, "j_captcha_error"),
        SERVER_BUSY(920, "server_busy"),

        // user 相关以 10 开头
        USER_NOT_EXISTS(1001, "user_not_exists"),
        USER_EMAIL_NOT_BIND(1047, "user_email_not_bind"),

        //team
        TEAM_NOT_EXIST(3700, "team_not_exist"),
        TEAM_MEMBER_NOT_EXISTS(3710, "team_member_not_exists"),
        TEAM_NOT_HAVE_PROJECT(3718, "team_not_have_project"),

        //charge
        TEAM_CHARGE_NOT_ADVANCED_PAY(3800, "team_charge_not_advanced_pay"),

        // project 相关以 11 开头
        PROJECT_NOT_EXIST(1100, "project_not_exists"),
        PROJECT_MEMBER_EXISTS(1101, "project_member_exists"),
        PROJECT_MEMBER_NOT_EXISTS(1102, "project_member_not_exists"),
        PROJECT_NAME_EXISTS(1103, "project_name_exists"),
        PROGRAM_START_AFTER_MILESTONE(1104, "program_start_date_after_milestone"),
        PROJECT_ALREADY_STARS(1106, "project_already_stars"),
        PROJECT_ALREADY_WATCH(1107, "project_already_watch"),
        PROJECT_OWNER_CANNOT_QUIT(1109, "project_owner_can_not_quit"),
        PROJECT_MEMBER_OVER(1110, "project_member_over"),
        PROJECT_FILE_SPACE_OVER(1111, "project_file_space_over"),
        TASK_STATUS_PARAM_ERROR(1115, "task_status_param_error"),
        PROJECT_DESCRIPTION_TOO_LONG(1116, "project_description_too_long", new Object[]{ValidationConstants.PROJECT_DESCRIPTION_MAX_LENGTH}),
        PROJECT_NAME_CONFLICT(1117, "project_name_conflict"),
        PROJECT_ICON_TOO_LARGE(1118, "project_icon_too_large"),
        UPDATE_PROJECT_ICON_ERROR(1119, "update_project_icon_error"),
        PROJECT_ICON_ERROR(1120, "project_icon_error"),
        PROJECT_IMAGE_BIG_THAN_10M(1121, "project_image_big_than_10m"),
        PROJECT_IMAGE_UPLOAD_ERROR(1122, "project_image_upload_error"),
        PROJECT_NOT_MEMBER(1123, "project_not_member"),
        PROJECT_PUBLIC_DENY_PROJECT_PERMISSION(1124, "project_public_deny_project_permission"),
        PROJECT_ALREADY_ARCHIVED(1125, "project_already_archived"),
        PROJECT_NOT_ARCHIVED(1126, "project_not_archived"),
        PROJECT_ARCHIVE_ERROR(1127, "project_archive_error"),
        PROJECT_UNARCHIVE_ERROR(1128, "project_unarchive_error"),
        PROJECT_UNARCHIVE_NAME_DUPLICATED(1129, "project_unarchive_name_duplicated"),
        PROJECT_IS_ARCHIVED(1130, "project_is_archived"),
        ALIAS_INVALID(1131, "alias_invalid"),
        PROJECT_OWNER_ONLY(1132, "project_owner_only"),
        PROJECT_FILE_ZIP_DOWNLOAD_FAILED(1133, "project_file_zip_download_failed"),
        PROJECT_FOLDER_NAME_DUPLICATE(1134, "project_folder_name_duplicate"),
        PROJECT_FOLDER_CANNOT_MV_TO_SUBFOLDER(1135, "project_folder_cannot_mv_to_subfolder"),
        PROJECT_FOLDERS_REQUIRED(1136, "project_folders_required"),
        PROJECT_DELETE_NO_EMPTY_FOLDER(1137, "project_delete_no_empty_folder"),
        PROJECT_TYPE_INVALID(1138, "project_type_invalid"),
        PROJECT_VCS_TYPE_INVALID(1139, "project_vcs_type_invalid"),
        PROJECT_FILE_NAME_DUPLICATE(1140, "project_file_name_duplicate"),
        PROJECT_FILE_MOVE_INTO_SELF(1141, "project_file_move_into_self"),
        PROJECT_FILE_ALL_IN_TARGET_FOLDER(1142, "project_file_all_in_target_folder"),
        PROJECT_FILE_RETAIN_CYCLE(1143, "project_file_retain_cycle"),
        PROJECT_QUICK_FOLDER_OPERATION_DISALLOW(1144, "project_quick_folder_operation_disallow"),
        FILE_NAME_TOO_LONG(1145, "file_name_too_long", new Object[]{PROJECT_FILE_NAME_LENGTH}),
        PROJECT_FILE_COMMENT_TOO_LONG(1, "project_file_comment_too_long"),
        PROJECT_NAME_ERROR(1, "project_name_error"),
        IMPORT_URL_INVALID(1, "import_url_invalid"),
        PROJECT_NAME_LENGTH_ERROR(1146, "project_name_length_error", new Object[]{PROJECT_NAME_MIN_LENGTH, PROJECT_NAME_MAX_LENGTH}),
        PROJECT_FILE_NAME_TOO_LONG(1147, "project_file_name_too_long"),
        TARGET_PROJECT_NOT_EXIST(1148, "target_project_not_exists"),
        PROJECT_START_DATE_ERROR(1148, "project_start_date_error"),
        PROJECT_END_DATE_ERROR(1149, "project_end_date_error"),
        PROJECT_END_DATE_BEFORE_START_DATE(1150, "project_end_date_before_start_date"),
        PROJECT_DEMO_NAME_EXISTS(1151, "project_demo_name_exists"),
        GENERATE_KEY_ERROR(1152, "generate_key_error"),
        DEPOT_IMPORT_FAILED(1153, "depot_import_failed"),
        ADD_OUTER_KEY_FAILED(1154, "add_outer_key_failed"),
        PROJECT_DISPLAY_NAME_IS_EMPTY(1160, "project_display_name_is_empty"),
        PROJECT_DISPLAY_NAME_EXISTS(1161, "project_display_name_exists"),
        PROJECT_DISPLAY_NAME_LENGTH_ERROR(1162, "project_display_name_length_error", new Object[]{PROJECT_DISPLAY_NAME_MIN_LENGTH, PROJECT_DISPLAY_NAME_MAX_LENGTH}),
        PROJECT_DISPLAY_NAME_ERROR(1163, "project_display_name_error"),
        PROJECT_CREATION_ERROR(1164, "project_creation_failed"),
        PROJECT_RESOURCE_CODE_AMOUNT_ERROR(1165, "project_resource_code_amount_error"),
        PROJECT_RESOURCE_CODE_IS_USED(1166, "project_resource_code_is_used"),
        PROJECT_RESOURCE_INSERT_EMPTY(1167, "project_resource_insert_empty"),
        DIR_NAME_TOO_LONG(1168, "dir_name_too_long", new Object[]{PROJECT_DIR_NAME_LENGTH}),
        PROJECT_DEMO_CREATE_FAILURE(1169, "project_demo_create_failure"),
        PROJECT_DEMO_RESET_LIMIT(1170, "project_demo_reset_limit"),
        PROJECT_TEMPLATE_NOT_EXIST(1171, "project_template_not_exist"),
        PROJECT_ARCHIVED_ONLY_ADMIN(1172, "project_archived_only_admin"),
        PROJECT_NAME_IS_EMPTY(1173, "project_name_not_empty"),
        PROJECT_NOT_EXIST_OR_ARCHIVED(1174, "project_not_exist_or_archived"),
        PROJECT_START_DATE_NOT_EMPTY(1175, "project_start_date_not_empty"),
        PROJECT_END_DATE_NOT_EMPTY(1176, "project_end_date_not_empty"),
        PROJECT_PERSONAL_PREFERENCE_KEYS_NOT_EXIST(1024, "project_personal_preference_keys_not_exist"),

        //group相关的以 15开头
        GROUP_NOT_EXIST(1500, "group_not_exist"),
        GROUP_USER_EXISTS(1501, "group_user_exists"),
        GROUP_MEMBER_NOT_EXIST(1502, "group_member_not_exist"),
        GROUP_TOPIC_TITLE_NOT_EMPTY(1503, "group_topic_title_not_empty"),
        GROUP_TOPIC_CONTENT_NOT_EMPTY(1504, "group_topic_CONTENT_not_empty"),

        // tweet相关的以 17 开头
        TWEET_COMMENT_CONTENT_NOT_EMPTY(1700, "tweet_comment_content_not_empty"),
        TWEET_REPEAT(1701, "tweet_repeat"),
        TWEET_FAST(1702, "tweet_fast"),
        TWEET_COMMENT_REPEAT(1703, "tweet_comment_repeat"),
        TWEET_LIKE_REPEAT(1704, "tweet_like_repeat"),
        TWEET_IMAGE_BIG_THAN_5M(1705, "tweet_image_big_than_5m"),
        TWEET_IMAGE_INSERT_ERROR(1706, "tweet_image_insert_error"),
        TWEET_IMAGE_LIMIT_N(1707, "tweet_image_limit_n"),
        TWEET_NOT_OWNER(1708, "tweet_not_owner"),
        TWEET_NOT_EXISTS(1709, "tweet_not_exists"),
        TWEET_ALREADY_RECOMMEND(1710, "tweet_already_recommend"),

        // project_tweet相关的以175开头
        PROJECT_TWEET_NOT_EXISTS(1751, "project_tweet_not_exists"),
        PROJECT_TWEET_REPEAT(1752, "project_tweet_repeat"),
        PROJECT_TWEET_FAST(1753, "project_tweet_fast"),

        //Image以19开头
        IMAGE_DOWNLOAD_ERROR(1900, "image_download_error"),

        // project pin
        PROJECT_PIN_LIMIT(2300, "project_pin_limit"),

        //project label
        LABEL_NAME_ERROR(2500, "label_name_error"),
        LABEL_EXIST(2501, "label_exist"),
        LABEL_COLOR_ERROR(2502, "label_color_error"),
        LABEL_NAME_TOO_LONG(2503, "label_name_too_long"),
        //permissions相关以 14 开头
        PERMISSION_DENIED(1400, "permission_denied"),

        DEPLOY_TOKEN_NOT_EXIST(7202, "deploy_token_not_exist"),
        DEPLOY_TOKEN_PROJECT_NOT_MATCH(7203, "deploy_token_project_not_match"),
        DEPLOY_TOKEN_CREATOR_NOT_MATCH(7204, "deploy_token_creator_not_match"),
        DEPLOY_TOKEN_PASSWORD_EMPTY(7205, "deploy_token_password_empty"),
        DEPLOY_TOKEN_STATUS_EMPTY(7206, "deploy_token_status_empty"),
        DEPLOY_TOKEN_SCOPE_EMPTY(7207, "deploy_token_scope_empty"),
        DEPLOY_TOKEN_SCOPE_INVALID(7208, "deploy_token_scope_invalid"),
        DEPLOY_TOKEN_NAME_TOO_LONG(7209, "deploy_token_name_too_long"),
        DEPLOY_TOKEN_USER_SCOPE_INVALID(7210, "deploy_token_user_scope_invalid"),
        DEPLOY_TOKEN_CREATE_FAIL(7212, "deploy_token_create_fail"),
        DEPLOY_TOKEN_NAME_EMPTY(7299, "deploy_token_name_empty"),
        DEPLOY_TOKEN_DISABLED(7300, "deploy_token_disabled"),
        DEPLOY_TOKEN_EXPIRED(7301, "deploy_token_expired"),
        ENTERPRISE_NOT_EXISTS(5003, "enterprise_not_exists"),
        //项目凭据
        CREDENTIAL_INSERT_ERROR(6601, "credential_insert_error"),
        CREDENTIAL_NOT_EXIST(6602, "credential_not_exist"),
        CREDENTIAL_DELETE_ERROR(6603, "credential_delete_error"),
        CREDENTIAL_UPDATE_ERROR(6604, "credential_update_error"),
        CREDENTIAL_TYPE_INVALID(6605, "credential_type_invalid"),
        CREDENTIAL_TASK_TYPE_INVALID(6606, "credential_task_type_invalid"),
        CREDENTIAL_PASSWORD_BYTES_TOO_LONG(6607, "credential_password_bytes_too_long"),
        CREDENTIAL_NAME_NOT_EMPTY(6608, "credential_name_not_empty"),
        CREDENTIAL_NAME_TOO_LONG(6609, "certificate_name_too_long"),
        CREDENTIAL_VERIFICATION_METHOD_NOT_EMPTY(6610, "credential_verification_method_not_empty"),
        CREDENTIAL_KUB_CONFIG_NOT_EMPTY(6611, "credential_kub_config_not_empty"),
        CREDENTIAL_URL_NOT_EMPTY(6612, "credential_kub_url_not_empty"),
        CREDENTIAL_SECRET_KEY_NOT_EMPTY(6613, "credential_secret_key_not_empty"),


        CI_JOB_NOT_FOUND(6301, "ci_job_not_found"),
        //全局资源
        GLOBAL_RESOURCE_SCOPE_TYPE_NOT_EXIST(8001, "global_resource_scope_type_not_exist"),
        // 用户项目配置
        USER_PROJECT_SETTING_CODE_NOT_EXISTS(8301, "user_project_setting_code_not_exists");

        private final int errorCode;
        private final String resourceKey;
        private Object[] args;
        // 初始化散列表的时候需要指定初始化容量，否则这里由于存入的容量过大，会导致不停 rehash，性能堪忧.
        // 这里考虑到现有容量以及未来扩容的情况，暂设定为 2^11=2048
        private static Map<Integer, ExceptionType> errorCodeLookupMap = new HashMap<>(1 << 11);
        private static Map<String, ExceptionType> resourceKeyLookupMap = new HashMap<>(1 << 11);

        static {
            for (ExceptionType e : values()) {
                errorCodeLookupMap.put(e.getErrorCode(), e);
                resourceKeyLookupMap.put(e.getResourceKey(), e);
            }
        }

        ExceptionType(int errorCode, String resourceKey) {
            this.errorCode = errorCode;
            this.resourceKey = resourceKey;
        }

        ExceptionType(int errorCode, String resourceKey, Object[] args) {
            this.errorCode = errorCode;
            this.resourceKey = resourceKey;
            this.args = args;
        }

        public int getErrorCode() {
            return errorCode;
        }

        public String getResourceKey() {
            return resourceKey;
        }

        public static ExceptionType findTypeByErrorCode(int errorCode) {
            return errorCodeLookupMap.get(errorCode);
        }

        public static ExceptionType findByResourceKey(String resourceKey) {
            return resourceKeyLookupMap.get(resourceKey);
        }

        public Object[] getArgs() {
            return args;
        }

        public void setArgs(Object[] args) {
            this.args = args;
        }

        public String[] getCodeAndKey() {
            return new String[]{
                    String.valueOf(this.errorCode),
                    this.resourceKey
            };
        }
    }

    private int code = 1;
    private String key = "";
    private String msg = "";
    private Map<String, String> map = new HashMap<>();
    private Map<String, Object> data = new HashMap<>();

    public CoreException(int code, String key, String msg) {
        this.code = code;
        this.key = key;
        this.msg = msg;
    }

    public CoreException(int code, String key, String msg, Map<String, Object> data) {
        this.code = code;
        this.key = key;
        this.msg = msg;
        this.data = data;
    }

    public CoreException() {
    }

    public CoreException(int code, Map<String, String> map) {
        this.code = code;
        this.map = map;
    }

    public String getMsg() {
        return msg;
    }

    public String getKey() {
        return key;
    }

    public int getCode() {
        return code;
    }

    public Map<String, String> getMap() {
        return map;
    }

    public Map<String, Object> getData() {
        return data;
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public Object handle(HttpServletRequest req, Exception ex) throws Exception {
        String origin = req.getHeader("Origin");
        String serverName = req.getServerName();
        boolean isAllow = AllowOriginUtil.isAllowOrigin(origin)
                || AllowOriginUtil.isAllowOriginAndServerName(origin, serverName);
        if (isAllow || isCrossOrigin(req)) {
            // Already add CORS on #SiteFilter
            return new ResponseEntity<>(handleToResult(req, ex), HttpStatus.OK);
        }
        // 给错误消息加上 CORS
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Access-Control-Allow-Origin", "*");
        headers.add("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS");
        headers.add("Access-Control-Max-Age", "3600");
        headers.add("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept, Access-Control-Allow-Headers, Authorization");
        headers.add("Access-Control-Allow-Credentials", "false");
        headers.add("Cache-Control", "no-cache, no-store");
        return new ResponseEntity<>(handleToResult(req, ex), headers, HttpStatus.OK);
    }

    private Result handleToResult(HttpServletRequest req, Exception ex) {
        if (log.isDebugEnabled()) {
            log.error(MessageFormat.format("core exception handle request {0} result", req.getRequestURL().toString()), ex);
        }
        if (ex instanceof CoreRuntimeException) {
            CoreRuntimeException coreRuntimeException = (CoreRuntimeException) ex;
            return failed(CoreException.of(coreRuntimeException.getExceptionType()));
        } else if (ex instanceof CoreException) {
            return failed((CoreException) ex);
        } else if (ex instanceof NoHandlerFoundException) {
            return buildResult(HttpServletResponse.SC_NOT_FOUND, "Not Found");
        } else if (ex instanceof HttpRequestMethodNotSupportedException) {
            return buildResult(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Method Not Allowed");
        } else if (ex instanceof HttpMediaTypeNotSupportedException) {
            return buildResult(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, "Unsupported Media Type");
        } else if (ex instanceof HttpMediaTypeNotAcceptableException) {
            return buildResult(HttpServletResponse.SC_NOT_ACCEPTABLE, "Not Acceptable");
        } else if (ex instanceof BindException) {
            log.error("catch exception", ex);
            BindException bindException = (BindException) ex;
            return failed(CoreException.of(bindException));
        } else if (ex instanceof ServletRequestBindingException
                || ex instanceof TypeMismatchException
                || ex instanceof HttpMessageNotReadableException
                || ex instanceof MethodArgumentNotValidException
                || ex instanceof MissingServletRequestPartException
                || ex instanceof InvalidPropertyException) {
            return buildResult(HttpServletResponse.SC_BAD_REQUEST, "Bad Request");
        } else if (ex instanceof CoreException.ExternalServiceInterruptException) {
            return buildResult(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "连接仓库失败，请稍后再试");
        } else {
            log.error("catch exception", ex);
            return failed(CoreException.of(ExceptionType.NETWORK_CONNECTION_ERROR));
        }
    }

    @ExceptionHandler(DBException.class)
    @ResponseBody
    public Object handleDBException(HttpServletRequest req, Exception ex) throws Exception {
        String message = ex.getMessage();
        String m1 = "Wrong number of parameters";
        String m2 = "Parameter metadata not available for these statement Query";
        if (message.contains(m1) || message.contains(m2)) {
            return new ResponseEntity<>(handleToResult(req, ex), new LinkedMultiValueMap<>(), HttpStatus.SERVICE_UNAVAILABLE);
        }

        return this.handle(req, ex);
    }

    private boolean isCrossOrigin(HttpServletRequest request) {
        Object o = request.getAttribute(CommonConstants.SKIP_EXCEPTION_CROSS_HEADER);
        if (o == null) {
            return false;
        }
        return (boolean) o;
    }

    public static Result failed(CoreException e) {
        // data 未设置时，则在接口的错误返回时，不返回
        Map<String, Object> data = e.getData().isEmpty() ? null : e.getData();

        if (e.getMap() != null && !e.getMap().isEmpty()) {
            return new Result(e.getCode(), e.getMap(), data);
        } else {
            HashMap<String, String> map = new HashMap<>();
            map.put(e.getKey(), e.getMsg());
            return new Result(e.getCode(), map, data);
        }
    }

    public static CoreException failed() {
        return CoreException.of(ExceptionType.DEFAULT_FAIL_EXCEPTION);
    }

    public boolean is(ExceptionType type) {
        return this.key.equals(type.getResourceKey()) && this.code == type.errorCode;
    }

    public static CoreException of(String key) {
        ExceptionType exceptionType;
        if ((exceptionType = ExceptionType.findByResourceKey(key)) != null) {
            return CoreException.of(exceptionType);
        } else {
            return new CoreException(1, key, getErrorText(key));
        }
    }

    public static CoreException of(Errors errors) {
        // build map (field : resource key)
        Map<String, String> fieldToResourceKeyMap = Result.buildMsg(errors);

        Map<String, Object[]> argsMap = new HashMap<>();
        for (FieldError fieldError : errors.getFieldErrors()) {
            Object[] args = fieldError.getArguments();
            if (args != null && args.length == 1) {
                argsMap.put(fieldError.getDefaultMessage(), args);
            }
        }

        // build map (field : resource string)
        Map<String, String> fieldToErrorMessageMap = new HashMap<>();
        for (Map.Entry<String, String> entry : fieldToResourceKeyMap.entrySet()) {
            ExceptionType type;
            if ((type = ExceptionType.findByResourceKey(entry.getValue())) != null) {
                if (argsMap.containsKey(entry.getValue())) {
                    fieldToErrorMessageMap.put(entry.getKey(), getErrorText(entry.getValue(), argsMap.get(entry.getValue())));
                } else {
                    fieldToErrorMessageMap.put(entry.getKey(), getErrorText(type.getResourceKey(), type.getArgs()));
                }
            } else {
                fieldToErrorMessageMap.put(entry.getKey(), getErrorText(entry.getValue()));
            }
        }

        // build Result object
        Iterator<String> iterator = fieldToResourceKeyMap.values().iterator();
        ExceptionType exceptionType;
        if (iterator.hasNext() && (exceptionType = ExceptionType.findByResourceKey(iterator.next())) != null) {
            return new CoreException(exceptionType.getErrorCode(), fieldToErrorMessageMap);
        } else {
            return new CoreException(ExceptionType.DEFAULT_FAIL_EXCEPTION.getErrorCode(), fieldToErrorMessageMap);
        }
    }

    public static String getErrorText(String key, Object... args) {
        //String text = ResourceUtils.getString("error", key);
        String text = ResourceUtil.error(key, args);
        text = QcloudUtils.officialDocument(text);
        if (args == null || args.length == 0) return text;
        return (text != null) ? MessageFormat.format(text, args) : null;
    }

    public static CoreException of(ExceptionType exceptionType) {
        return new CoreException(
                exceptionType.getErrorCode(),
                exceptionType.getResourceKey(),
                getErrorText(exceptionType.getResourceKey(), exceptionType.getArgs())
        );
    }

    public static CoreException of(ExceptionType exceptionType, Object... args) {
        return new CoreException(
                exceptionType.getErrorCode(),
                exceptionType.getResourceKey(),
                getErrorText(exceptionType.getResourceKey(), args)
        );
    }

    public static CoreException of(String key, Object... args) {
        ExceptionType exceptionType;
        if ((exceptionType = ExceptionType.findByResourceKey(key)) != null) {
            return CoreException.of(exceptionType, args);
        } else {
            return new CoreException(1, key, getErrorText(key));
        }
    }

    public static CoreException withData(ExceptionType exceptionType, Map<String, Object> data) {
        return new CoreException(
                exceptionType.getErrorCode(),
                exceptionType.getResourceKey(),
                getErrorText(exceptionType.getResourceKey(), exceptionType.getArgs()),
                data
        );
    }

    private static class ExternalServiceInterruptException extends RuntimeException {

        public ExternalServiceInterruptException(String message) {
            super(message);
        }
    }

    private Result buildResult(int code, String message) {
        Map<String, String> msg = new HashMap<>();
        msg.put(String.valueOf(code), message);
        return new Result(Result.CODE_FAILED, msg, null);
    }

    @Override
    public String getMessage() {
        return String.format("code: %d, key: %s, msg: %s", code, key, msg);
    }
}
