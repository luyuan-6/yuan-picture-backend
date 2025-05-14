package com.luyuan.yuanpicturebackend.controller;

import com.luyuan.yuanpicturebackend.Exeception.BusinessException;
import com.luyuan.yuanpicturebackend.Exeception.ErrorCode;
import com.luyuan.yuanpicturebackend.annotation.AuthCheck;
import com.luyuan.yuanpicturebackend.common.BaseResponse;
import com.luyuan.yuanpicturebackend.common.ResultUtils;
import com.luyuan.yuanpicturebackend.constant.UserConstant;
import com.luyuan.yuanpicturebackend.manager.CosManager;
import com.luyuan.yuanpicturebackend.model.dto.picture.PictureUploadRequest;
import com.luyuan.yuanpicturebackend.model.entity.Picture;
import com.luyuan.yuanpicturebackend.model.entity.User;
import com.luyuan.yuanpicturebackend.model.vo.PictureVO;
import com.luyuan.yuanpicturebackend.service.PictureService;
import com.luyuan.yuanpicturebackend.service.UserService;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.utils.IOUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

@RestController
@RequestMapping("/picture")
@Slf4j
public class PictureController {

    @Resource
    private UserService userService;

    @Resource
    private PictureService  pictureService;


    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/upload")
    public BaseResponse<PictureVO> testUploadFile(@RequestPart("file")MultipartFile multipartFile,
                                                  PictureUploadRequest pictureUploadRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

}
