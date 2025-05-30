package com.luyuan.yuanpicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.luyuan.yuanpicturebackend.Exeception.BusinessException;
import com.luyuan.yuanpicturebackend.Exeception.ErrorCode;
import com.luyuan.yuanpicturebackend.constant.UserConstant;
import com.luyuan.yuanpicturebackend.model.dto.user.UserLoginRequest;
import com.luyuan.yuanpicturebackend.model.dto.user.UserQueryRequest;
import com.luyuan.yuanpicturebackend.model.dto.user.UserRegisterRequest;
import com.luyuan.yuanpicturebackend.model.dto.user.UserUpdateRequest;
import com.luyuan.yuanpicturebackend.model.entity.User;
import com.luyuan.yuanpicturebackend.model.enums.UserRoleEnum;
import com.luyuan.yuanpicturebackend.model.vo.LoginUserVO;
import com.luyuan.yuanpicturebackend.model.vo.UserVO;
import com.luyuan.yuanpicturebackend.service.UserService;
import com.luyuan.yuanpicturebackend.mapper.UserMapper;
import com.luyuan.yuanpicturebackend.utils.ValidationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.luyuan.yuanpicturebackend.constant.UserConstant.USER_LOGIN_STATE;

/**
* @author luyuan
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2025-05-09 15:26:05
*/
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

    @Override
    public long userRegister(UserRegisterRequest userRegisterRequest) {
        // 1. 参数校验
        if (userRegisterRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();

        if (userAccount.length() < 4 ) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }

        if (userPassword.length() < 8 ) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }

        if (!userPassword.equals(checkPassword) ) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        // 2. 检查是否重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        Long count = this.baseMapper.selectCount(queryWrapper);
        if (count > 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
        }
        // 3. 加密
        String encryptPassword = getEncryptPassword(userPassword);
        // 4. 插入数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserName("无名");
        user.setUserRole(UserRoleEnum.USER.getValue());
        boolean saveResult = this.save(user);
        if (!saveResult){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "注册失败, 数据插入失败");
        }
        return user.getId();
    }

    @Override
    public boolean userUpdatePersonalInfo(UserUpdateRequest userUpdateRequest, HttpServletRequest request) {
        // 1. 校验参数
        if (userUpdateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        // 先校验是否是本人
        Long id = userUpdateRequest.getId();
        User loginUser = getLoginUser(request);
        if (!Objects.equals(id, loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅限本人可以修改个人信息");
        }

        String website = userUpdateRequest.getWebsite();
        String phone = userUpdateRequest.getPhone();
        String email = userUpdateRequest.getEmail();

        if (!ValidationUtils.isValidEmail(email)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱格式错误");
        }
        if (!ValidationUtils.isValidPhoneNumber(phone)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "电话格式错误");
        }
        if (!ValidationUtils.isValidWebsite(website)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "上传的个人网站不合法");
        }

//        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
//        updateWrapper.eq(User::getId, id);
//
//        if (CharSequenceUtil.isNotBlank(userUpdateRequest.getUserName())) {
//            updateWrapper.set(User::getUserName, userUpdateRequest.getUserName());
//        }
//        if (CharSequenceUtil.isNotBlank(userUpdateRequest.getUserAvatar())) {
//            updateWrapper.set(User::getUserAvatar, userUpdateRequest.getUserAvatar());
//        }
//        if (CharSequenceUtil.isNotBlank(userUpdateRequest.getUserProfile())) {
//            updateWrapper.set(User::getUserProfile, userUpdateRequest.getUserProfile());
//        }
//        if (CharSequenceUtil.isNotBlank(userUpdateRequest.getEmail())) {
//            updateWrapper.set(User::getEmail, userUpdateRequest.getEmail());
//        }
//        if (CharSequenceUtil.isNotBlank(userUpdateRequest.getPhone())) {
//            updateWrapper.set(User::getPhone, userUpdateRequest.getPhone());
//        }
//        if (CharSequenceUtil.isNotBlank(userUpdateRequest.getField())) {
//            updateWrapper.set(User::getField, userUpdateRequest.getField());
//        }
//        if (CharSequenceUtil.isNotBlank(userUpdateRequest.getGender())) {
//            updateWrapper.set(User::getGender, userUpdateRequest.getGender());
//        }
//        if (CharSequenceUtil.isNotBlank(userUpdateRequest.getCoverImage())) {
//            updateWrapper.set(User::getCoverImage, userUpdateRequest.getCoverImage());
//        }
//        if (CharSequenceUtil.isNotBlank(userUpdateRequest.getWebsite())) {
//            updateWrapper.set(User::getWebsite, userUpdateRequest.getWebsite());
//        }
//        if (CharSequenceUtil.isNotBlank(userUpdateRequest.getSkills())) {
//            updateWrapper.set(User::getSkills, userUpdateRequest.getSkills());
//        }
//        if (CharSequenceUtil.isNotBlank(userUpdateRequest.getCategories())) {
//            updateWrapper.set(User::getCategories, userUpdateRequest.getCategories());
//        }
//        int update = this.baseMapper.update(null, updateWrapper);
        // 构建更新条件
        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getId, id);

        User user = new User();

        BeanUtil.copyProperties(userUpdateRequest, user, true);// 忽略 null 值字段
        user.setId(id);
        user.setSkills(JSONUtil.toJsonStr(userUpdateRequest.getSkills()));
        user.setCategories(JSONUtil.toJsonStr(userUpdateRequest.getCategories()));
        boolean result = this.updateById(user);
        if (!result){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "修改失败, 数据插入失败");
        }

        return true;
    }

    @Override
    public LoginUserVO userLogin(UserLoginRequest userLoginRequest, HttpServletRequest request) {
        // 1. 校验
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        if (CharSequenceUtil.hasBlank(userAccount, userPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号错误");
        }
        if (userPassword.length() < 8){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }
        // 2. 加密
        String encryptPassword = getEncryptPassword(userPassword);

        // 查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = this.baseMapper.selectOne(queryWrapper);
        // 判断用户是否存在
        if (user == null){
            log.info("user login failed, userAccount cannot match userPassword");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }

        // 3.记录用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        return this.getLoginUserVO(user);
    }

    @Override
    public boolean userLogout(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        if (userObj == null){
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
        }
        // 移除登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtil.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream()
                .map(this::getUserVO)
                .collect(Collectors.toList());
    }

    public static void main(String[] args) {
        User user = new User();
        user.setId(11111L);
        user.setUserAccount("ss");
        user.setUserPassword("sss");
        user.setUserName("sssss");
        user.setUserRole("user");

        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
    }
    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long id = userQueryRequest.getId();
        String userName = userQueryRequest.getUserName();
        String userAccount = userQueryRequest.getUserAccount();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ObjectUtil.isNotNull(id),"id", id);
        queryWrapper.eq(StrUtil.isNotBlank(userRole),"userRole", userRole);
        queryWrapper.like(StrUtil.isNotBlank(userAccount), "userAccount", userAccount);
        queryWrapper.like(StrUtil.isNotBlank(userName), "userName", userName);
        queryWrapper.like(StrUtil.isNotBlank(userProfile), "userProfile", userProfile);
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 从数据库查询（追求性能的话可以注释，直接返回上述结果）
        long userId = currentUser.getId();
        currentUser = this.getById(userId);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }

    @Override
    public String getEncryptPassword(String password) {
        // 盐值
        final String SALT = "luyuan";
        return DigestUtils.md5DigestAsHex((SALT + password).getBytes());
    }

    @Override
    public boolean isAdmin(User user) {
        return user != null && UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
    }
}




