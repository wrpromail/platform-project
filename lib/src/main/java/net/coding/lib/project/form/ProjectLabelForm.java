package net.coding.lib.project.form;

import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.constraints.Pattern;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class ProjectLabelForm {

    @NotBlank(message = "label_color_is_empty")
    @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$", message = "label_color_error")
    private String color;

    @NotBlank(message = "label_name_error")
    @Length(max = 36, message = "label_name_too_long")
    private String name;

    private Integer projectId;

}
