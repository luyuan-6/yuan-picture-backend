package com.luyuan.yuanpicturebackend.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class UserVO implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * id
     */
    private Long id;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户角色：user/admin
     */
    private String userRole;

    /**
     * 会员过期时间
     */
    private Date vipExpireTime;

    /**
     * 会员兑换码
     */
    private String vipCode;

    /**
     * 会员编号
     */
    private Long vipNumber;

    /**
     * 创建时间
     */
    private Date createTime;

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
    private String skills;

    /**
     * 用户擅长素材分类(json 数组)
     */
    private String categories;



}
