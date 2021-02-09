package net.coding.lib.project.form;


import org.hibernate.validator.constraints.NotEmpty;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by liuying on 2021/2/04.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddMemberForm {

    @NotEmpty(message = "param_error")
    private String users;

    private List<Integer> targetUserIds;
    private String[] targetUserIdStrArray;
    @Builder.Default
    private short type = 80;


}
