package com.luyuan.yuanpicturebackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.luyuan.yuanpicturebackend.Exeception.BusinessException;
import com.luyuan.yuanpicturebackend.Exeception.ErrorCode;
import com.luyuan.yuanpicturebackend.Exeception.ThrowUtils;
import com.luyuan.yuanpicturebackend.annotation.AuthCheck;
import com.luyuan.yuanpicturebackend.common.BaseResponse;
import com.luyuan.yuanpicturebackend.common.DeleteRequest;
import com.luyuan.yuanpicturebackend.common.ResultUtils;
import com.luyuan.yuanpicturebackend.constant.UserConstant;
import com.luyuan.yuanpicturebackend.model.dto.user.*;
import com.luyuan.yuanpicturebackend.model.entity.User;
import com.luyuan.yuanpicturebackend.model.enums.UserRoleEnum;
import com.luyuan.yuanpicturebackend.model.vo.LoginUserVO;
import com.luyuan.yuanpicturebackend.model.vo.UserVO;
import com.luyuan.yuanpicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);
        long result = userService.userRegister(userRegisterRequest);
        return ResultUtils.success(result);
    }
    /**
     * 用户登录
     */
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
        LoginUserVO userLoginVO = userService.userLogin(userLoginRequest, request);
        return ResultUtils.success(userLoginVO);
    }

    /**
     * 获取当前登录用户
     */
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(userService.getLoginUserVO(loginUser));
    }

    /**
     * 用户注销
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        boolean result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    /**
     * 用户修改个人信息
     */
    @PostMapping("/update/personalInfo")
    public BaseResponse<Boolean> userUpdatePersonalInfo(@RequestBody UserUpdateRequest userUpdateRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(userUpdateRequest == null, ErrorCode.PARAMS_ERROR);
        boolean result = userService.userUpdatePersonalInfo(userUpdateRequest, request);
        return ResultUtils.success(result);
    }

    /**
     * 创建用户
     * @param userAddRequest
     * @return
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        ThrowUtils.throwIf(userAddRequest == null, ErrorCode.PARAMS_ERROR);
        User user = new User();
        BeanUtils.copyProperties(userAddRequest, user);
        // 填充信息
        final String DEFAULT_PASSWORD = "12345678";
        String encryptPassword = userService.getEncryptPassword(DEFAULT_PASSWORD);
        user.setUserPassword(encryptPassword);
        boolean save = userService.save(user);
        ThrowUtils.throwIf(!save, ErrorCode.PARAMS_ERROR, "注册失败, 数据插入失败");
        return ResultUtils.success(user.getId());
    }


    /**
     * 根据id获取用户(仅管理员)
     * @param id
     * @return
     */
    @PostMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(@RequestParam(required = false) Long id) {
        // 先判空
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "ID 不能为空");
        }
        // 再判断值是否合法
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "ID 必须大于 0");
        }
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        return ResultUtils.success(user);
    }

    /**
     * 根据id获取脱敏用户
     * @param id
     * @return
     */
    @PostMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(@RequestParam(required = false) Long id) {
        // 先判空
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "ID 不能为空");
        }
        // 再判断值是否合法
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "ID 必须大于 0");
        }
        BaseResponse<User> response = getUserById(id);
        User user = response.getData();
        return ResultUtils.success(userService.getUserVO(user));
    }

    /**
     * 删除用户
     * @param deleteRequest
     * @return
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = userService.removeById(deleteRequest.getId());
        return ResultUtils.success(result);
    }

    /**
     * 更新用户
     * @param userUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        if (userUpdateRequest == null || userUpdateRequest.getId() <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = new User();
        BeanUtils.copyProperties(userUpdateRequest, user);
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.PARAMS_ERROR, "更新用户信息失败");
        return ResultUtils.success(true);
    }

    /**
 * 分页获取用户封装列别(仅管理员)
 * @param userQueryRequest 查询请求参数
 * @return
 */
@PostMapping("/list/page/vo")
@AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest) {
    // 参数校验，确保查询请求不为空
    ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);

    // 获取分页参数
    long current = userQueryRequest.getCurrent();
    long pageSize = userQueryRequest.getPageSize();

    // 执行用户分页查询
    Page<User> userPage = userService.page(new Page<>(current, pageSize), userService.getQueryWrapper(userQueryRequest));

    // 初始化用户VO分页对象
    Page<UserVO> userVOPage = new Page<>(current, pageSize, userPage.getTotal());

    // 将查询到的用户对象列表转换为用户VO列表
    List<UserVO> userVOList = userService.getUserVOList(userPage.getRecords());
    userVOPage.setRecords(userVOList);

    // 返回分页结果
    return ResultUtils.success(userVOPage);
}

}
