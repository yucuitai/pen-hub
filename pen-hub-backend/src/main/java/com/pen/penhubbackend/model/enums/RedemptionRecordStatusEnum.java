package com.pen.penhubbackend.model.enums;

import lombok.Getter;

/**
 * 兑换记录状态枚举
 */
@Getter
public enum RedemptionRecordStatusEnum {

    SUCCESS("SUCCESS", "兑换成功"),
    FAILED("FAILED", "兑换失败");

    private final String value;
    private final String description;

    RedemptionRecordStatusEnum(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public static RedemptionRecordStatusEnum getByValue(String value) {
        if (value == null) {
            return null;
        }
        for (RedemptionRecordStatusEnum statusEnum : values()) {
            if (statusEnum.getValue().equals(value)) {
                return statusEnum;
            }
        }
        return null;
    }
}
