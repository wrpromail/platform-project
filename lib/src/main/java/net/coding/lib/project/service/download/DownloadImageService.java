package net.coding.lib.project.service.download;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import net.coding.common.util.HttpUtils;
import net.coding.common.util.IPUtils;
import net.coding.common.util.MimeUtil;
import net.coding.lib.project.grpc.client.StorageGrpcClient;
import net.coding.lib.project.utils.RedisUtil;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DownloadImageService {

    @Autowired
    private CodingSettings codingSettings;

    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024;

    @Value("${coding-net-public-image:coding-static-bucket-public}")
    private String bucket;

    private final static String ORIGIN_SRC = "origin-src";
    private final static String SRC = "src";
    private final static String IMAGE = "img";
    private final static String A_TAG = "a";
    private final static String A_HREF = "href";

    private String notFoundImage = "https://coding.net/static/no-pic.png";
    private String domainPrefix1 = "https://dn-coding-";
    private String domainPrefix2 = "https://coding-";
    private String subDomain = "coding";

    private static Map mimeSuffixMap = ImmutableMap.builder()
            .put("image/gif", "gif")
            .put("image/jpeg", "jpg")
            .put("image/png", "png")
            .put("image/bmp", "bmp")
            .put("image/x-ms-bmp", "bmp")
            .put("image/vnd.microsoft.icon", "ico")
            .build();
    private static Collection<String> mimeSuffixSet = Lists.newArrayList(
            "gif",
            "jpg",
            "jpeg",
            "png",
            "bmp",
            "ico"
    );
    private static ListeningExecutorService executor = MoreExecutors.listeningDecorator(
            Executors.newFixedThreadPool(200)
    );
    private static Pattern URL_PATTERN = Pattern.compile("^((https|http|ftp|rtsp|mms)?://)+(([0-9a-z_!~*'().&=+$%-]+:" +
                    " )" +
                    "?[0-9a-z_!~*'().&=+$%-]+@)?(([0-9]{1,3}\\.){3}[0-9]{1," +
                    "3}|([0-9a-z_!~*'()-]+\\.)*([0-9a-z][0-9a-z-]{0," +
                    "61})?[0-9a-z]\\.[a-z]{2,6})(:[0-9]{1,4})?((/?)|(/[0-9a-z_!~*'().;?:@&=+$,%#-]+)+/?)$",
            Pattern.CASE_INSENSITIVE
    );

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private StorageGrpcClient storageGrpcClient;

    /**
     * 下载图片
     */
    public String downloadImage(String imageUrl, String bucket) {
        if (!validateUrl(imageUrl)) {
            return codingSettings.getApp().getImage().getNotFoundImage();
        }
        final InputStream inputStream = HttpUtils.doGet(imageUrl, HttpUtils.USER_AGENT, 15 * 1000);
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            if (inputStream == null) {
                return codingSettings.getApp().getImage().getNotFoundImage();
            }

            byte[] data = IOUtils.toByteArray(inputStream);

            if (null == data) {
                return codingSettings.getApp().getImage().getNotFoundImage();
            }

            if (data.length > MAX_IMAGE_SIZE) {
                return codingSettings.getApp().getImage().getNotFoundImage();
            }

            String mime = MimeUtil.parse(data);
            StringBuilder sb = new StringBuilder();
            sb.append(UUID.randomUUID().toString()).append(".");
            String suffix = String.valueOf(mimeSuffixMap.get(mime));
            if (null == suffix) {
                return codingSettings.getApp().getImage().getNotFoundImage();
            }
            sb.append(suffix);

            String key = UUID.randomUUID().toString() + "." + FilenameUtils.getExtension(sb.toString());
            byte[] content = IOUtils.toByteArray(inputStream);
            Boolean result = storageGrpcClient.store(key, content, bucket);
            if (result) {
                return storageGrpcClient.getDownloadUrl(bucket, key);
            }
            return null;
        } catch (IOException e) {
            log.error("download image(" + imageUrl + ") failed", e.getCause());
            return codingSettings.getApp().getImage().getNotFoundImage();
        } catch (Exception exception) {
            log.error("download image(" + imageUrl + ") failed, exception={}", exception);
            return codingSettings.getApp().getImage().getNotFoundImage();
        } finally {
            IOUtils.closeQuietly(outputStream);
        }
    }

    public static boolean validateUrl(String imageUrl) {
        return IPUtils.isUrlLookingForwardToPublicNetworkIp(imageUrl);
    }

    /**
     * 将外部图片下载到七牛
     */
    public String filterHTML(String content) {
        Document doc = Jsoup.parse(content);
        Elements images = doc.getElementsByTag(IMAGE);

        List<ListenableFuture<Element>> futures = new ArrayList<>();

        if (CollectionUtils.isEmpty(images)) {
            return doc.body().html();
        }

        images.forEach(image -> {
            futures.add(executor.submit(new DownloadRunner(image, bucket)));
        });

        try {
            // 等待线程完成
            Futures.successfulAsList(futures).get(images.size() * 10, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error("download images failed", e.getCause());
        }
        return doc.body().html();
    }

    /**
     * 判断当前的url是不是合法的图片资源url
     */
    private static boolean validateImageUrl(String url) {
        if ("".equals(url)) {
            return false;
        }
        Matcher matcher = URL_PATTERN.matcher(url);
        if (!matcher.find()) {
            return false;
        }

        String suffix = url.substring(url.lastIndexOf('.') + 1).toLowerCase();
        Iterator<String> i = mimeSuffixSet.iterator();
        while (i.hasNext()) {
            if (suffix.equals(i.next())) {
                return true;
            }
        }
        return false;
    }

    private String downloadImageAndPersist(String imageUrl, String bucket) {
        // 需要判断图片大小，暂时先不处理
        URL urlWithCodingHost;
        try {
            urlWithCodingHost = new URL(imageUrl);
        } catch (MalformedURLException e) {
            log.info("图片链接异常：" + imageUrl);
            return codingSettings.getApp().getImage().getNotFoundImage();
        }
        Pattern subDomain = Pattern.compile("^\\S+\\." + codingSettings.getApp().getImage().getSubDomain() + "\\.\\S+$");
        Matcher matcher = subDomain.matcher(urlWithCodingHost.getHost());
        if (matcher.matches() ||
                imageUrl.startsWith(codingSettings.getApp().getImage().getDomainPrefix1()) ||
                imageUrl.startsWith(codingSettings.getApp().getImage().getDomainPrefix2())
        ) {
            return imageUrl;
        }
        if (!validateImageUrl(imageUrl)) {
            return codingSettings.getApp().getImage().getNotFoundImage();
        }
        String url = redisUtil.get("image:" + imageUrl);
        if (StringUtils.isEmpty(url)) {
            url = downloadImage(imageUrl, bucket);
            redisUtil.set("image:" + imageUrl, url);
        }
        return url;
    }

    /**
     * 构造传递的 image 对象直接会在此修改 src 等属性
     */
    private class DownloadRunner implements Callable<Element> {

        private Element image;
        private String bucket;

        public DownloadRunner(Element image, String bucket) {
            this.image = image;
            this.bucket = bucket;
        }

        @Override
        public Element call() throws Exception {

            String imageUrl = image.attr(SRC);
            image.attr(ORIGIN_SRC, imageUrl);
            if (StringUtils.isBlank(imageUrl)) {
                log.debug("image url {} is blank, skipped", imageUrl);
                return image;
            }
            if (StringUtils.startsWith(imageUrl, "/")) {
                log.debug("image url {} is relative link, skipped", imageUrl);
                return image;
            }
            String url = downloadImageAndPersist(imageUrl, bucket);
            image.attr(SRC, url);
            return image;
        }
    }
}
