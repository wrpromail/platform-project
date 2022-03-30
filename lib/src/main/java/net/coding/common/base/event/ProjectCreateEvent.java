package net.coding.common.base.event;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class ProjectCreateEvent {
    private Integer projectId;

    private Map<String, String> initMap;

    private Boolean fork;
    private int parentId;
    private int rootId;
    @Builder.Default
    private int quota = 102400;

    // 创建团队项目时，项目 owner 直接设置为了团队，当初始化 depot 时，如果直接拿 owner 来初始化，会导致初始化
    // 的用户跟创建项目的用户不一致，
    //
    // 所以当创建团队项目的时候，需要主动传这个 creator，当 depot 判断到这个 creator 不为 null 的时候，
    // 用这个用户来进行初始化
    private Integer userId;

    private String vcsType;
    private boolean shared;
    // 创建项目时是否初始化仓库
    private Boolean initDepot;
}
