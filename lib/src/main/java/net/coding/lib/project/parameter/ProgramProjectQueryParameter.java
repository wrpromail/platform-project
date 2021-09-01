package net.coding.lib.project.parameter;

import java.sql.Timestamp;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class ProgramProjectQueryParameter {

    private Integer teamId;

    private Integer userId;

    private Integer programId;

    private Timestamp deletedAt;

    private String queryType;

}
