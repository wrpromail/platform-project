package net.coding.lib.project.metrics;

import io.prometheus.client.Gauge;

public class ProjectCreateMetrics {

    public enum Type {
        ALL,
        INIT_PROJECT_DATA,
        PROJECT_CREATE_EVENT
    }


    public static final Gauge projectCreateTimeConsumingRate = Gauge.build()
            .name("project_create_time_consuming")
            .help("Project Create time consuming(all,init_project_data,project_create_event)")
            .labelNames("type")
            .register();

    public static void setAll(long value) {
        projectCreateTimeConsumingRate
                .labels(Type.ALL.name().toLowerCase())
                .set(value);
    }

    public static void setInitProjectData(long value) {
        projectCreateTimeConsumingRate
                .labels(Type.INIT_PROJECT_DATA.name().toLowerCase())
                .set(value);
    }

    public static void setProjectCreateEvent(long value) {
        projectCreateTimeConsumingRate
                .labels(Type.PROJECT_CREATE_EVENT.name().toLowerCase())
                .set(value);
    }

}
