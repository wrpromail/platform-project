package net.coding.lib.project.infra;

import net.coding.grpc.client.platform.infra.text.moderation.TextModerationInfraGrpcClient;
import net.coding.grpc.client.platform.infra.text.moderation.TextProto.TextModerationRequest;
import net.coding.grpc.client.platform.infra.text.moderation.TextProto.TextModerationResponse;
import net.coding.platform.degradation.annotation.Degradation;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
@Service
public class TextModerationService {

    private final static String LABEL_NORMAL = "Normal";
    private final static String LABEL_PORN = "Porn";
    private final static String LABEL_ABUSE = "Abuse";
    private final static String LABEL_AD = "Ad";
    private final static String LABEL_ILLEGAL = "Illegal";
    //自定义违规
    private final static String LABEL_CUSTOM = "Custom";
    private final static String SUGGESTION_PASS = "Pass";
    private final static String SUGGESTION_BLOCK = "Block";
    private final static String SUGGESTION_REVIEW = "Review";
    private final TextModerationInfraGrpcClient textModerationInfraGrpcClient;

    /**
     * 文本内容安全检查 返回所有关键字
     */
    private TextModerationResponse audit(String content) {
        TextModerationResponse response = textModerationInfraGrpcClient.audit(
                TextModerationRequest.newBuilder()
                        .setContent(content)
                        .build());
        if (!response.getSuggestionPass()) {
            log.error("Audit content exception, label:{}, suggestion: {}, keywords:{}",
                    response.getLabel(),
                    response.getSuggestion(),
                    response.getKeywordsList());
        }
        return response;
    }

    /**
     * 返回关键字
     */
    @Degradation
    public String checkContent(String content) {
        if (StringUtils.isEmpty(content)) {
            return StringUtils.EMPTY;
        }
        TextModerationResponse response = audit(content);
        // 包含限制词
        List<String> keywords = response == null ? Collections.emptyList() : response.getKeywordsList();
        return keywords.size() > 0 ? keywords.get(0) : StringUtils.EMPTY;
    }
}
