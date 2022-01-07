package net.coding.lib.project.form;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectFilterForm {
    private String team;
    private TeamType teamType = TeamType.all;
    private QueryType type = QueryType.all;

    public enum TeamType {
        all,
        in,
        relevant
    }

    public enum QueryType {
        all,
        joined,
        managed
    }
}
