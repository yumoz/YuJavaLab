package com.example.entity;

public enum UserType {
    NORMAL(0),
    ADMIN(1),
    VIP(2);

    private final int code;

    UserType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static UserType fromCode(int code) {
        for (UserType type : UserType.values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown UserType code: " + code);
    }
}
