package com.luyuan.yuanpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.luyuan.yuanpicturebackend.model.dto.user.UserLoginRequest;
import com.luyuan.yuanpicturebackend.model.dto.user.UserQueryRequest;
import com.luyuan.yuanpicturebackend.model.dto.user.UserRegisterRequest;
import com.luyuan.yuanpicturebackend.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.luyuan.yuanpicturebackend.model.vo.LoginUserVO;
import com.luyuan.yuanpicturebackend.model.vo.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author luyuan
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2025-05-09 15:26:05
*/
public interface UserService extends IService<User> {

    /**
     * 用户注册
     * @param userRegisterRequest
     * @return
     */
    long userRegister(UserRegisterRequest userRegisterRequest);

    /**
     * 用户登录
     * @param userLoginRequest
     * @param request
     * @return
     */
    LoginUserVO userLogin(UserLoginRequest userLoginRequest, HttpServletRequest request);

    boolean userLogout(HttpServletRequest request);
    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 获取脱敏的已登录用户信息
     *
     * @return
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 获取脱敏的用户信息
     *
     * @return
     */
    UserVO getUserVO(User user);

    /**
     * 获取脱敏的用户列表
     *
     * @return
     */
    List<UserVO> getUserVOList(List<User> userList);

    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);
    /**User user
     * 获取加密密码
     * @param password
     * @return
     */
    String getEncryptPassword(String password);

    /**
     * 是否为管理员
     * @param user
     * @return
     */
    boolean isAdmin(User user);
}
