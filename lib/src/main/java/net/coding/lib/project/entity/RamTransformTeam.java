package net.coding.lib.project.entity;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "ram_transform_team")
public class RamTransformTeam {

    @Id
    @GeneratedValue(generator = "JDBC")
    private Long id;
    private Long teamId;
    private String status;
    private String message;

    @Getter
    public enum Status {
        ReadWrite_None(0),     // 迁移前
        Read_None(10),          // 迁移中
        ReadWrite_Write(50),    // 双写
        ReadWrite_ReadWrite(60),     // 双写 - 读新
        Read_ReadWrite(80),
        None_ReadWrite(100);      // 上线

        private int value;

        Status(int value) {
            this.value = value;
        }
    }
}
