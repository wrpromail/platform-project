package net.coding.common.base.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProgramEvent {

    private Integer teamId;

    private Integer projectId;

    private Integer userId;

    private Function function;


    public enum Function {
        Create,
        DELETE,
        ARCHIVE,
        UNARCHIVE
    }
}
