package com.luyuan.yuanpicturebackend.model.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户的技能
 */
@Data
public class Skill implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 技能名称
     */
    private String name;

    /**
     * 技能等级(0 - 100)
     */
    private int level;

    /**
     * 颜色
     */
    private String color;
}
