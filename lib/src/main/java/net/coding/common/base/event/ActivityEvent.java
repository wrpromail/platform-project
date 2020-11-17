package net.coding.common.base.event;

import net.coding.common.ParametrizedEventType;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class ActivityEvent implements ParametrizedEventType {
    private Integer creatorId;

    private Class type;

    private Integer targetId;

    private Short action;

    private String content;

    private Integer projectId;

    @Override
    public String toString() {
        return "ActivityEvent{" +
                "creatorId=" + creatorId +
                ", type=" + type +
                ", targetId=" + targetId +
                ", action=" + action +
                ", content='" + content + '\'' +
                ", projectId=" + projectId +
                '}';
    }


    @Override
    public List<Type> provideParametrized() {
        if(type == null) {
            return Collections.emptyList(); //最终还是会 post 失败
        }
        return Collections.singletonList(type);
    }
}
