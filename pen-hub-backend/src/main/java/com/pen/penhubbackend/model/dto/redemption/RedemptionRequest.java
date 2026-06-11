package com.pen.penhubbackend.model.dto.redemption;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户兑换请求
 */
@Data
public class RedemptionRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 兑换码
     */
    private String code;
}
