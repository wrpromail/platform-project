package net.coding.app.project.metric;

import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Summary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
@Slf4j
public class MetricsProvider {

    public static Counter requestTotal;
    public static Counter requestFailedTotal;
    public static Gauge requestStatus;
    public static Summary requestLatency;

    private String name = "e_platform_project";

    @PostConstruct
    public void init() {
        log.info("init() 初始化prometheus!");
        requestTotal = Counter.build()
                .name(name + "_request_total")
                .labelNames( "module", "service", "action")
                .help("the total of request")
                .register();

        requestFailedTotal = Counter.build()
                .name(name + "_request_failed_total")
                .labelNames("module","service", "action")
                .help("the total of failed request")
                .register();

        requestStatus = Gauge.build()
                .name(name + "_request_status")
                .labelNames("module", "service","action")
                .help("the status of request")
                .register();

        requestLatency = Summary.build()
                .quantile(0.5, 0.05)
                .quantile(0.9, 0.01)
                .name(name + "_request_latency")
                .help("Request latency in seconds.")
                .register();
    }

}