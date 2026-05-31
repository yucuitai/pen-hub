package com.pen.penhubbackend.model.enums;

import lombok.Getter;

/**
 * 用户角色枚举
 */
@Getter
public enum UserRoleEnum {

    USER("user", "普通用户"),
    ADMIN("admin", "管理员");

    private final String value;
    private final String description;

    UserRoleEnum(String value, String description) {
        this.value = value;
        this.description = description;
    }

    /**
     * 根据值获取枚举
     */
    public static UserRoleEnum getByValue(String value) {
        for (UserRoleEnum role : values()) {
            if (role.getValue().equals(value)) {
                return role;
            }
        }
        return null;
    }
}
