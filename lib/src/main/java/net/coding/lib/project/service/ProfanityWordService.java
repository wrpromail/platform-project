package net.coding.lib.project.service;

import java.lang.reflect.Field;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coding.common.base.form.BaseForm;
import net.coding.common.base.validator.IncludeProfanity;
import net.coding.lib.project.infra.TextModerationService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;

@Service
@Slf4j
@AllArgsConstructor
public class ProfanityWordService {

    private final TextModerationService textModerationService;

    public void process(Errors errors, BaseForm form) {

        Field[] fields = form.getClass().getDeclaredFields();
        try {
            for (Field field : fields) {
                if (reject(errors, form, field)) {
                    return;
                }
            }
        } catch (IllegalAccessException e) {
            log.error("Abnormal Verification of Sensitive Words! {}", e.getMessage());
        }
    }

    /**
     * 解析 form 每个字段并校验
     *
     * @param errors errors
     * @param form   form
     * @param field  field
     * @return 是否命中敏感词，true：命中，false：没命中
     * @throws IllegalAccessException IllegalAccessException
     */
    private boolean reject(Errors errors, BaseForm form, Field field)
            throws IllegalAccessException {
        if (field.isAnnotationPresent(IncludeProfanity.class)) {
            IncludeProfanity includeProfanity = field.getAnnotation(IncludeProfanity.class);
            field.setAccessible(true);
            String fieldName = field.getName();
            String key = includeProfanity.message();
            String value = String.valueOf(field.get(form));
            String profanityWord = validate(value);
            if (StringUtils.isNotBlank(profanityWord)) {
                form.rejectValueWithArgs(errors, fieldName, key, new Object[]{profanityWord});
                return true;
            }
        }
        return false;
    }

    /**
     * 敏感词校验
     *
     * @param word 字符串
     * @return 命中的敏感词
     */
    public String validate(String word) {
        return textModerationService.checkContent(word);
    }
}
