package com.pen.penhubbackend.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 兑换码视图对象
 */
@Data
public class RedemptionCodeVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 兑换码ID
     */
    private Long id;

    /**
     * 兑换码
     */
    private String code;

    /**
     * 产品类型
     */
    private String productType;

    /**
     * 产品描述
     */
    private String productDescription;

    /**
     * 最大使用次数
     */
    private Integer maxUses;

    /**
     * 已使用次数
     */
    private Integer usedCount;

    /**
     * 状态
     */
    private String status;

    /**
     * 状态描述
     */
    private String statusDescription;

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 描述
     */
    private String description;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
