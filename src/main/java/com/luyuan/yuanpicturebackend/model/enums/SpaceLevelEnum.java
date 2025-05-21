package com.luyuan.yuanpicturebackend.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum SpaceLevelEnum {
    COMMON("普通版", 0, 100, 100L * 1024 * 1024),
    PROFESSIONAL("专业版", 1, 1000, 1000L * 1024 * 1024),
    FLAGSHIP("旗舰版", 2, 10000, 10000L * 1024 * 1024);

    private final String text;

    private final int value;

    private final long maxCount;

    private final long maxSize;

    SpaceLevelEnum(String text, int value, long maxCount, long maxSize) {
        this.text = text;
        this.value = value;
        this.maxCount = maxCount;
        this.maxSize = maxSize;
    }
    public static SpaceLevelEnum getValueByText(Integer  text) {
        if (ObjUtil.isEmpty(text)) {
            return null;
        }
        for (SpaceLevelEnum value : values()) {
            if (value.getText().equals(text)) {
                return value;
            }
        }
        return null;
    }

}
