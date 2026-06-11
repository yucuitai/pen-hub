package com.pen.penhubbackend.model.enums;

import lombok.Getter;

/**
 * 兑换码状态枚举
 */
@Getter
public enum RedemptionStatusEnum {

    ACTIVE("ACTIVE", "可用"),
    DISABLED("DISABLED", "已禁用"),
    EXHAUSTED("EXHAUSTED", "已用完");

    private final String value;
    private final String description;

    RedemptionStatusEnum(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public static RedemptionStatusEnum getByValue(String value) {
        if (value == null) {
            return null;
        }
        for (RedemptionStatusEnum statusEnum : values()) {
            if (statusEnum.getValue().equals(value)) {
                return statusEnum;
            }
        }
        return null;
    }
}
