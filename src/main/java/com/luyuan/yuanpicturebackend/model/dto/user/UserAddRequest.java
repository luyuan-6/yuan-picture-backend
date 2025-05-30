package com.luyuan.yuanpicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户创建请求
 */
@Data
public class UserAddRequest implements Serializable {

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户角色: user, admin
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


    private static final long serialVersionUID = 1L;
}