package net.coding.lib.project.dto;

import net.coding.common.util.TextUtils;
import net.coding.lib.project.entity.Project;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Optional;

import javax.xml.ws.BindingType;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Created by jack on 14-4-25.
 */
@Builder
public class ProjectDTO {

    private String description;
    private Integer id;
    private String name;
    private String display_name;
    private String start_date;
    private String end_date;
    private String icon;

}
