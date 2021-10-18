package net.coding.lib.project.event;

import net.coding.common.base.bean.setting.BaseSetting;

import java.util.Collection;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class NotificationEvent {
    private Collection<Integer> userIds;
    private String content;
    private String targetType;
    private String targetId;
    private Class<? extends BaseSetting> baseSettingClass;
}
