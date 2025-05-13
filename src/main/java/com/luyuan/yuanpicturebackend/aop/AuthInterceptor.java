package com.luyuan.yuanpicturebackend.aop;

import com.luyuan.yuanpicturebackend.Exeception.BusinessException;
import com.luyuan.yuanpicturebackend.Exeception.ErrorCode;
import com.luyuan.yuanpicturebackend.annotation.AuthCheck;
import com.luyuan.yuanpicturebackend.model.entity.User;
import com.luyuan.yuanpicturebackend.model.enums.UserRoleEnum;
import com.luyuan.yuanpicturebackend.service.UserService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Aspect
@Component
public class AuthInterceptor {

    @Resource
    private UserService userService;

    @Around("@annotation(authCheck)")
    public Object  doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        String mustRole = authCheck.mustRole();
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        // 获取当前用户
        User loginUser = userService.getLoginUser(request);
        UserRoleEnum mustRoleEnum = UserRoleEnum.getEnumByValue(mustRole);
        // 不需要权限 放行
        if (mustRoleEnum == null){
            return joinPoint.proceed();
        }
        // 以下 必须要用权限才能通过
        // 获取当前用户具有的权限
        String userRole = loginUser.getUserRole();
        UserRoleEnum currentUserRoleEnum = UserRoleEnum.getEnumByValue(userRole);
        // 没有权限 拒绝
        if (currentUserRoleEnum == null ){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 要求必须有管理员权限 但用户没有管理员权限拒绝
        if (currentUserRoleEnum != UserRoleEnum.ADMIN && mustRoleEnum == UserRoleEnum.ADMIN){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 通过权限校验 放行
        return joinPoint.proceed();
    }
}
