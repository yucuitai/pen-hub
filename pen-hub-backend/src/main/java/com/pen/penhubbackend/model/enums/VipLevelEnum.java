package com.pen.penhubbackend.model.enums;

import lombok.Getter;

/**
 * VIP 等级枚举
 */
@Getter
public enum VipLevelEnum {

    NORMAL(0, "普通用户"),
    BASIC(1, "基础版"),
    PRO(2, "专业版"),
    ULTIMATE(3, "旗舰版");

    private final int level;
    private final String description;

    VipLevelEnum(int level, String description) {
        this.level = level;
        this.description = description;
    }

    /**
     * 根据等级值获取枚举
     */
    public static VipLevelEnum getByLevel(int level) {
        for (VipLevelEnum e : values()) {
            if (e.getLevel() == level) {
                return e;
            }
        }
        return NORMAL;
    }

    /**
     * 是否为 VIP（任意等级）
     */
    public boolean isVip() {
        return this.level > 0;
    }

    /**
     * 是否达到指定等级
     */
    public boolean isAtLeast(VipLevelEnum required) {
        return this.level >= required.getLevel();
    }
}
