package net.coding.lib.project.entity;

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "merge_request_labels")
@Data
public class MergeRequestLabel {

    @Id
    @GeneratedValue(generator = "JDBC")
    @Column(name = "`id`", updatable = false, nullable = false)
    private Integer id;

    private Integer mergeRequestId;

    private Integer labelId;

    private Timestamp createdAt;

    private Timestamp updatedAt;

    private Timestamp deletedAt;

}
