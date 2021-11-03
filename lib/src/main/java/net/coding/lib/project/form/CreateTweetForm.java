package net.coding.lib.project.form;

import net.coding.common.base.form.BaseForm;
import net.coding.common.util.TextUtils;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.validation.Errors;

import lombok.Data;
import lombok.EqualsAndHashCode;

import static net.coding.common.base.validator.ValidationConstants.TWEET_LIMIT_CODES;
import static net.coding.common.base.validator.ValidationConstants.TWEET_LIMIT_CODE_LENGTH;
import static net.coding.common.base.validator.ValidationConstants.TWEET_LIMIT_IMAGES;

@Data
@EqualsAndHashCode(callSuper = false)
public class CreateTweetForm extends BaseForm {

    private String content;
    private String slateRaw;

    @Override
    public boolean supports(Class<?> clazz) {
        return CreateTweetForm.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {

        if (errors.getFieldErrorCount() > 0) {
            return;
        }
        if (limitTweetImages(content, TWEET_LIMIT_IMAGES)) {
            rejectValue(errors, "content", "tweet_image_limit_n");
        }
        if (limitTweetCodes(content, TWEET_LIMIT_CODES)) {
            rejectValue(errors, "content", "tweet_codes_limit_n");
        }
        if (limitTweetCodeLength(content, TWEET_LIMIT_CODE_LENGTH)) {
            rejectValue(errors, "content", "tweet_code_length_limit_n");
        }
        if (StringUtils.isEmpty(TextUtils.filterUserInputContent(content))) {
            rejectValue(errors, "content", "tweet_content_after_filter_not_empty");
        }
    }

    /**
     * 判断用户冒泡的图片数量是否超限
     */
    public boolean limitTweetImages(String content, int amount) {
        Document doc = Jsoup.parse(content);
        Elements eles = doc.select("img.bubble-markdown-image");
        return eles.size() > amount;
    }

    /**
     * 判断用户冒泡的代码块数量是否超限
     */
    public boolean limitTweetCodes(String content, int amount) {
        Document doc = Jsoup.parse(content);
        Elements eles = doc.select("code");
        return eles.size() > amount;
    }

    public boolean limitTweetCodeLength(String content, int length) {
        int total = getTweetCodeLength(content);
        return total > length;
    }

    private int getTweetCodeLength(String content) {
        Document doc = Jsoup.parse(content);
        Elements elements = doc.select("code");
        int total = 0;
        for (Element element : elements) {
            total += element.html().length();
        }
        return total;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
