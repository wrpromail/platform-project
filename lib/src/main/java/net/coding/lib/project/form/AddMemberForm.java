package net.coding.lib.project.form;


import org.apache.commons.lang.math.NumberUtils;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.validation.Errors;

import java.util.ArrayList;
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
    private short type = 80;


}
