package net.coding.lib.project.infra;

import net.coding.grpc.client.platform.infra.text.pinyin.PinyinInfraGrpcClient;
import net.coding.grpc.client.platform.infra.text.pinyin.PinyinProto.PinyinRequest;
import net.coding.grpc.client.platform.infra.text.pinyin.PinyinProto.PinyinResponse;
import net.coding.grpc.client.platform.infra.text.pinyin.PinyinProto.PinyinStyle;
import net.coding.platform.degradation.annotation.Degradation;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static org.apache.commons.lang3.StringUtils.EMPTY;

@Slf4j
@AllArgsConstructor
@Service
public class PinyinService {
    public final static String DEFAULT_SEPARATOR = "|";
    private final static Pattern CH = Pattern.compile("[\\u4e00-\\u9fa5]");

    private final PinyinInfraGrpcClient pinyinInfraGrpcClient;

    private String normal(String value) {
        PinyinResponse response = pinyinInfraGrpcClient.convert(
                PinyinRequest.newBuilder()
                        .setValue(value)
                        .setMixed(false)
                        .setHeteronym(false)
                        .setSeparator(EMPTY)
                        .setStyle(PinyinStyle.NORMAL)
                        .build());
        if (response.getValueCount() < 1) {
            return "";
        }
        return String.join(EMPTY, response.getValueList());
    }

    /**
     *
     */
    private String combined(String value, String separator) {
        PinyinResponse response = pinyinInfraGrpcClient
                .convert(PinyinRequest.newBuilder()
                        .setValue(value)
                        .setSeparator(DEFAULT_SEPARATOR)
                        .setMixed(true)
                        .setStyle(PinyinStyle.NORMAL)
                        .setHeteronym(chineseLen(value) <= 5)
                        .build());
        if (response == null || response.getValueCount() < 1) {
            return null;
        }
        return String.join(separator, response.getValueList());
    }

    private long chineseLen(String value) {
        if (StringUtils.isBlank(value)) {
            return 0;
        }

        return value
                .chars()
                .filter(c -> CH.matcher(String.valueOf((char) c)).matches())
                .count();
    }

    @Degradation
    public String getPinYin(String displayName, String name) {
        return combined(
                StringUtils.defaultIfBlank(
                        displayName,
                        name
                ),
                PinyinService.DEFAULT_SEPARATOR
        );
    }
}
