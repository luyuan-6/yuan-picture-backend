package com.luyuan.yuanpicturebackend.model.dto.user;

import com.luyuan.yuanpicturebackend.model.entity.Category;
import com.luyuan.yuanpicturebackend.model.entity.Skill;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 更新用户请求
 */
@Data
public class UserUpdateRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 简介
     */
    private String userProfile;

    /**
     * 用户角色：user/admin
     */
    private String userRole;

    /**
     * 用户个人网站
     */
    private String website;


    /**
     * 用户电话号码
     */
    private String phone;

    /**
     * 用户邮箱
     */
    private String email;

    /**
     * 用户创作领域
     */
    private String field;

    /**
     * 用户性别
     */
    private String gender;

    /**
     * 用户个人信息背景
     */
    private String coverImage;

    /**
     * 用户专业技能(json 数组)
     */
    private List<Skill> skills;

    /**
     * 用户擅长素材分类(json 数组)
     */
    private List<Category> categories;


    private static final long serialVersionUID = 1L;
}