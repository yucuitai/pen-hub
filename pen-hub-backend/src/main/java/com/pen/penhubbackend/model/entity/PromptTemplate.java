package com.pen.penhubbackend.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Prompt 模板实体
 *
 * @author pen-hub
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(value = "prompt_template")
public class PromptTemplate implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;

    /** 模板名称 */
    private String name;

    /** 所属智能体名称 */
    private String agentName;

    /** Prompt 内容 */
    private String promptContent;

    /** 版本号 */
    private Integer version;

    /** 状态：0-禁用 1-启用 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 是否删除 */
    @Column(value = "is_delete", isLogicDelete = true)
    private Integer isDelete;
}
