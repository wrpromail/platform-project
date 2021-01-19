package net.coding.lib.project.service;

import net.coding.common.base.form.BaseForm;
import net.coding.common.base.validator.IncludeProfanity;
import net.coding.lib.project.dao.ProfanityWordDao;
import net.coding.lib.project.entity.ProfanityWord;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;

@Service
@Slf4j
@AllArgsConstructor
public class ProfanityWordService {

    private static final List<ProfanityWord> list = new ArrayList<>();
    private static long lastUpdate = System.currentTimeMillis();
    private static long timeout = MILLISECONDS.convert(1, MINUTES);

    @PostConstruct
    public void init() {
        update();
    }

    private final ProfanityWordDao profanityWordDao;

    public String checkContent(String content) {
        if (StringUtils.isEmpty(content)) {
            return "";
        }
        checkAndUpdate();
        for (ProfanityWord profanityWord : list) {
            if (content.toLowerCase().contains(profanityWord.getWord().toLowerCase()))
                return profanityWord.getWord();
        }
        return "";
    }

    private void update() {
        lastUpdate = System.currentTimeMillis();
        List<ProfanityWord> words = profanityWordDao.findAll();
        synchronized (list) {
            list.clear();
            list.addAll(words);
        }
    }

    private void checkAndUpdate() {
        if (System.currentTimeMillis() - lastUpdate > timeout) {
            update();
        }
    }

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
    private boolean reject(Errors errors, BaseForm form, Field field) throws IllegalAccessException {
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
        return checkContent(word);
    }
}
