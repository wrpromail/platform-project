package net.coding.lib.project.utils;

import com.google.common.html.types.SafeUrl;
import com.google.common.html.types.SafeUrls;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;

import java.net.URI;
import java.net.URISyntaxException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TextUtil {

    public static final Whitelist user_content_filter = new Whitelist()
            .addTags(
                    "a", "b", "blockquote", "br", "cite", "code", "dd", "dl", "dt", "em", "i", "li", "ol", "p",
                    "pre", "q", "small", "strike", "strong", "sub", "sup", "u", "ul", "img", "span", "div", "h1",
                    "h2", "h3", "h4", "h5", "h6", "table", "tbody", "tr", "th", "td", "hr", "del", "tt", "input")
            .addAttributes("a", "href", "title", "target", "class", "raw")
            .addAttributes("blockquote", "cite")
            .addAttributes("q", "cite")
            .preserveRelativeLinks(true)
            .addProtocols("blockquote", "cite", "http", "https")
            .addEnforcedAttribute("a", "target", "_blank")
            .addAttributes("img", "align", "alt", "height", "src", "title", "width", "border", "align", "class")
            .addAttributes("h1", "id")
            .addAttributes("h2", "id")
            .addAttributes("h3", "id")
            .addAttributes("h4", "id")
            .addAttributes("h5", "id")
            .addAttributes("h6", "id")
            .addAttributes("a", "id")
            .addAttributes("pre", "class")
            .addAttributes("code", "class")
            .addAttributes("div", "class", "align")
            .addAttributes("span", "class", "style") // 这里先允许 style，后面还会过滤，见下面的 clean 方法
            .addAttributes("table", "border", "bordercolor", "cellpadding", "cellspacing", "align")
            .addAttributes("th", "align", "rowspan", "colspan")
            .addAttributes("td", "align", "rowspan", "colspan")
            .addAttributes("ul", "class")
            .addAttributes("li", "class")
            .addAttributes("input", "type", "class", "checked", "disabled");

    public static final String DEPRECATED_QCLOUD_DOMAIN = "https://qcloud.coding.net";


    /**
     * 截取指定范围的字符串
     * @param originalString
     * @param length
     * @param appendEllipsis
     * @return
     */
    public static String substr(String originalString, int length, boolean appendEllipsis) {
        if (originalString == null) {
            return null;
        }

        if (originalString.length() <= length) {
            return originalString;
        }

        String result = originalString.substring(0 , appendEllipsis ? (length - 1) : length);

        if (Boolean.TRUE.equals(appendEllipsis)) {
            result += "…";
        }

        return result;
    }

    /**
     * 截取固定中文字符长度（两个英文算一个中文字符）的字符串，原字符串长度不足会返回原字符串。
     *
     * @param length         中文字符的长度
     * @param appendEllipsis 是否在截取后的字符串尾部添加省略号（仅在原字符串长度足够时有效）
     */
    public static String getFixedLengthString(String originalString, int length, boolean appendEllipsis) {
        if (originalString == null) return null;
        if (originalString.length() <= length) return originalString;
        StringBuilder buf = new StringBuilder();
        int len = originalString.length();
        int wc = 0;
        int ncount = 2 * length;
        if (appendEllipsis)
            ncount = ncount - 3;
        for (int i = 0; i < len; ) {
            if (wc >= ncount) break;
            char ch = originalString.charAt(i++);
            buf.append(ch);
            wc += 2;
            if (wc >= ncount) break;
            if (CharUtils.isAscii(ch)) {
                wc -= 1;
                //read next char
                if (i >= len) break;
                char nch = originalString.charAt(i++);
                buf.append(nch);
                if (!CharUtils.isAscii(nch))
                    wc += 2;
                else
                    wc += 1;
            }
        }
        if (appendEllipsis)
            buf.append("...");
        return buf.toString();
    }

    /**
     * 获取指定HTML标签的指定属性的值最后一个值, 未找到返回默认值
     * @param html
     * @param element
     * @param attr
     * @param defaultRes
     * @return
     */
    public static String getAttrValByLastElement(String html, String element, String attr, String defaultRes){
        Elements elements = Jsoup.parse(html).getElementsByTag(element);
        if (elements.size() == 0) {
            return defaultRes;
        }
        return elements.get(elements.size() - 1).attr(attr);
    }

    /**
     * 从一段HTML中萃取纯文本
     */
    public static String getPlainText(String html) {
        if (StringUtils.isBlank(html)) return "";
        return Jsoup.parse(html).text();
    }

    public static String htmlEscape(String text) {
        if (text == null || "".equals(text)) {
            return "";
        } else {
            text = text
                    .replaceAll("<", "&lt;")
                    .replaceAll(">", "&gt;");
            String[] wmPhone = text.split("-WM");
            return wmPhone[0];
        }
    }

    public static String filterUserInputContent(String html) {
        if (StringUtils.isBlank(html)) return "";
        return clean(html, "");
    }

    public static String filterGitContentWithHostWithProtocol(String html, String hostWithProtocol) {
        if (StringUtils.isBlank(html)) return "";
        if (StringUtils.isBlank(hostWithProtocol)) return "";
        return clean(html, hostWithProtocol);
    }

    public static String replaceImageDomainInMarkdown(String markdownText, String sourceHostWithProtocol, String targetHostWithProtocol) {
        String result =  markdownText.replace("![图片](" + sourceHostWithProtocol, "![图片](" + targetHostWithProtocol);
        result = result.replace("![图片](" + DEPRECATED_QCLOUD_DOMAIN, "![图片](" + targetHostWithProtocol);

        return result;
    }

    public static String replaceImagePreviewDomain(String html, String sourceHostWithProtocol, String targetHostWithProtocol) {
        String result = replaceImgSrcDomain(
                html,
                sourceHostWithProtocol,
                targetHostWithProtocol
        );

        result = replaceImgSrcDomain(
                result,
                DEPRECATED_QCLOUD_DOMAIN,
                targetHostWithProtocol
        );

        result = replaceImgAtagHrefDomain(
                result,
                sourceHostWithProtocol,
                targetHostWithProtocol
        );

        result = replaceImgAtagHrefDomain(
                result,
                DEPRECATED_QCLOUD_DOMAIN,
                targetHostWithProtocol
        );

        return result;
    }

    public static String replaceImgSrcDomain(String html, String sourceHostWithProtocol, String targetHostWithProtocol) {
        try {
            if (StringUtils.isBlank(sourceHostWithProtocol) || StringUtils.isBlank(targetHostWithProtocol)) {
                return html;
            }

            URI sourceURI = new URI(sourceHostWithProtocol);
            URI targetURI = new URI(targetHostWithProtocol);

            Document document = Jsoup.parseBodyFragment(html, sourceHostWithProtocol);
            Elements elements = document.getElementsByTag("img");
            if (CollectionUtils.isEmpty(elements)) {
                return html;
            }

            elements.forEach(i -> {
                String imgSrc = i.attr("src");
                String originSrc = i.attr("origin-src");
                if (StringUtils.isBlank(imgSrc)) {
                    return;
                }

                URI imgSrcURI;
                URI originSrcURI;
                try {
                    imgSrcURI = new URI(imgSrc);
                    originSrcURI = new URI(originSrc);
                } catch (URISyntaxException e) {
                    log.warn("Can't parse url " + imgSrc);
                    return;
                }

                if (imgSrcURI.getScheme() != null
                        && imgSrcURI.getHost() != null
                        && imgSrcURI.getScheme().equals(sourceURI.getScheme())
                        && imgSrcURI.getHost().equals(sourceURI.getHost())) {
                    replaceLink(i, "src", targetURI);
                }

                if (originSrcURI.getScheme() != null
                        && originSrcURI.getHost() != null
                        && originSrcURI.getScheme().equals(sourceURI.getScheme())
                        && originSrcURI.getHost().equals(sourceURI.getHost())) {
                    replaceLink(i, "origin-src", targetURI);
                }
            });
            return document.body().html();
        } catch (URISyntaxException e) {
            log.error("Cannot convert html to targetHostWithProtocol.", e);
            return html;
        }
    }

    public static String replaceImgAtagHrefDomain(String html, String sourceHostWithProtocol, String targetHostWithProtocol) {
        try {
            if (StringUtils.isBlank(sourceHostWithProtocol) || StringUtils.isBlank(targetHostWithProtocol)) {
                return html;
            }

            URI sourceURI = new URI(sourceHostWithProtocol);
            URI targetURI = new URI(targetHostWithProtocol);

            Document document = Jsoup.parseBodyFragment(html, sourceHostWithProtocol);
            Elements elements = document.getElementsByTag("a");
            if (CollectionUtils.isEmpty(elements)) {
                return html;
            }

            elements.forEach(i -> {
                String hrefSrc = i.attr("href");
                if (StringUtils.isBlank(hrefSrc)) {
                    return;
                }

                URI hrefSrcURI;
                try {
                    hrefSrcURI = new URI(hrefSrc);
                } catch (URISyntaxException e) {
                    log.warn("Can't parse url " + hrefSrc);
                    return;
                }

                if (hrefSrcURI.getScheme() != null
                        && hrefSrcURI.getHost() != null
                        && hrefSrcURI.getScheme().equals(sourceURI.getScheme())
                        && hrefSrcURI.getHost().equals(sourceURI.getHost())
                        && hrefSrcURI.getPath().endsWith("imagePreview")) {
                    replaceLink(i, "href", targetURI);
                }
            });
            return document.body().html();
        } catch (URISyntaxException e) {
            log.error("Cannot convert html to targetHostWithProtocol.", e);
            return html;
        }
    }

    public static String replaceAtagHrefDomain(String html, String sourceHostWithProtocol, String targetHostWithProtocol) {
        try {
            if (StringUtils.isBlank(sourceHostWithProtocol) || StringUtils.isBlank(targetHostWithProtocol)) {
                return html;
            }

            URI sourceURI = new URI(sourceHostWithProtocol);
            URI targetURI = new URI(targetHostWithProtocol);

            Document document = Jsoup.parseBodyFragment(html, sourceHostWithProtocol);
            Elements elements = document.getElementsByTag("a");
            if (CollectionUtils.isEmpty(elements)) {
                return html;
            }

            elements.forEach(i -> {
                String hrefSrc = i.attr("href");
                if (StringUtils.isBlank(hrefSrc)) {
                    return;
                }

                URI hrefSrcURI;
                try {
                    hrefSrcURI = new URI(hrefSrc);
                } catch (URISyntaxException e) {
                    log.warn("Can't parse url " + hrefSrc);
                    return;
                }

                if (hrefSrcURI.getScheme() != null
                        && hrefSrcURI.getHost() != null
                        && hrefSrcURI.getScheme().equals(sourceURI.getScheme())
                        && hrefSrcURI.getHost().equals(sourceURI.getHost())
                ) {
                    replaceLink(i, "href", targetURI);
                }
            });
            return document.body().html();
        } catch (URISyntaxException e) {
            log.error("Cannot convert html to targetHostWithProtocol.", e);
            return html;
        }
    }

    public static Element replaceLink(Element element, String attr, URI hostURI) {
        try {
            URI uri = new URI(element.attr(attr));
            element.attr(attr, new URI(hostURI.getScheme(), hostURI.getHost(), uri.getPath(), uri.getRawQuery(), uri.getFragment()).toString());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return element;
    }


    private static String clean(String html, String baseUri) {
        Document dirty = Jsoup.parseBodyFragment(html, baseUri);
        Cleaner cleaner = new Cleaner(user_content_filter);
        Document clean = cleaner.clean(dirty);
        // 只允许 type 为 checkbox 的 input 标签, task list 在用
        clean.getElementsByTag("input")
                .stream()
                .filter(el -> !StringUtils.equals(el.attr("type").toLowerCase(), "checkbox"))
                .forEach(Element::remove);
        // 检查链接 href 是否合法，将不合法 href 转换为安全链接
        clean.getElementsByTag("a")
                .stream()
                .filter(el -> {
                    //a标签有锚点的去掉target="_blank"属性
                    if(el.attr("href").startsWith("#")){
                        el.removeAttr("target");
                    }
                    String href = el.attr("href").toLowerCase();
                    return StringUtils.isNotBlank(href);
                })
                .forEach(element -> {
                    String href = element.attr("href");
                    SafeUrl safeUrl = SafeUrls.sanitize(href);
                    element.attr("href", safeUrl.getSafeUrlString());
                });

        // 移除非 .kdmath 下 span 标签的 style 属性
        clean.select("span:not(.kdmath span)").forEach(element -> element.removeAttr("style"));
        return clean.body().html();
    }

    public static boolean containsEmoji(String source) {
        if (StringUtils.isBlank(source)) {
            return false;
        }

        int len = source.length();

        for (int i = 0; i < len; i++) {
            char codePoint = source.charAt(i);

            if (!isNotEmojiCharacter(codePoint)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isNotEmojiCharacter(char codePoint) {
        return (codePoint == 0x0) ||
                (codePoint == 0x9) ||
                (codePoint == 0xA) ||
                (codePoint == 0xD) ||
                ((codePoint >= 0x20) && (codePoint <= 0xD7FF)) ||
                ((codePoint >= 0xE000) && (codePoint <= 0xFFFD)) ||
                ((codePoint >= 0x10000) && (codePoint <= 0x10FFFF));
    }

    /**
     * 过滤emoji 或者 其他非文字类型的字符
     */
    public static String filterEmojiCharacter(String source) {

        if (!containsEmoji(source)) {
            return source;//如果不包含，直接返回
        }
        //到这里铁定包含
        StringBuilder buf = null;

        int len = source.length();

        for (int i = 0; i < len; i++) {
            char codePoint = source.charAt(i);

            if (isNotEmojiCharacter(codePoint)) {
                if (buf == null) {
                    buf = new StringBuilder(source.length());
                }

                buf.append(codePoint);
            }
        }

        if (buf == null) {
            return "";
        } else {
            if (buf.length() == len) {//这里的意义在于尽可能少的toString，因为会重新生成字符串
                buf = null;
                return source;
            } else {
                return buf.toString();
            }
        }

    }

    public static boolean isMarkup(String file_name) {
        return StringUtils.endsWithAny(file_name.toLowerCase(), new String[]{
                ".rdoc", ".org", ".creole", "mediawiki", ".wiki", ".litcoffee",
                ".restructuredtext", ".rst", ".rest", ".rst.txt", ".rest.txt",
                ".adoc", ".asciidoc", ".asc", ".pod", ".rmd", ".textile"
        });
    }

    public static boolean isMarkdown(String file_name) {
        return file_name.toLowerCase().endsWith(".markdown")
                || file_name.toLowerCase().endsWith(".md")
                || file_name.toLowerCase().endsWith(".mdown");
    }

    public static String imageAndCodeToWord(String content) {
        Document document = TextUtil.replaceImagesTag(content);;
        Elements codes = document.getElementsByTag("code");
        for (Element code : codes) {
            code.replaceWith(new TextNode("[代码]", ""));
        }
        return document.body().html();
    }

    public static String imageAndCodeToWord(String content, int numLimit) {
        String ret = imageAndCodeToWord(content);
        ret = TextUtil.getPlainText(ret);
        ret = TextUtil.htmlEscape(ret);
        ret = TextUtil.getFixedLengthString(ret, numLimit, true);

        return ret;
    }

    /**
     * 把 html 中的图片和 emotion 表情替换为文字.
     */
    public static Document replaceImagesTag(String content) {
        Document document = Jsoup.parse(content);
        Elements images = document.getElementsByTag("img");
        for (Element image : images) {
            if (image.hasClass("emotion")) {
                image.replaceWith(new TextNode("[表情]", StringUtils.EMPTY));
            } else {
                image.replaceWith(new TextNode("[图片]", StringUtils.EMPTY));
            }
        }
        return document;
    }

    public static String abstractHtml(String content, Integer length) {
        Document document = replaceImagesTag(content);
        return getFixedLengthString(
                TextUtil.htmlEscape(
                        TextUtil.getPlainText(document.body().html())),
                length,
                true);
    }
}
