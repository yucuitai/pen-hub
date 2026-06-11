package com.pen.penhubbackend.model.dto.redemption;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 管理员创建兑换码请求
 */
@Data
public class RedemptionCodeCreateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 产品类型：VIP_MONTHLY/VIP_YEARLY
     */
    private String productType;

    /**
     * 创建数量，最多10个
     */
    private Integer count;

    /**
     * 描述
     */
    private String description;
}
