package com.pen.penhubbackend.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 兑换记录视图对象
 */
@Data
public class RedemptionRecordVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 记录ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

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
     * 状态
     */
    private String status;

    /**
     * 状态描述
     */
    private String statusDescription;

    /**
     * 会员到期时间
     */
    private LocalDateTime expireTime;

    /**
     * 描述
     */
    private String description;

    /**
     * 兑换时间
     */
    private LocalDateTime createTime;
}
