package net.coding.lib.project.entity;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;

@Data
public class ProfanityWord implements Serializable {

    private static final long serialVersionUID = 1000000000006L;

    private Integer id;

    private Date createdAt;

    private Date deletedAt;

    private String word;
}