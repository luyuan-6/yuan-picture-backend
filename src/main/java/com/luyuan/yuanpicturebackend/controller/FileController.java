package com.luyuan.yuanpicturebackend.controller;

import com.luyuan.yuanpicturebackend.Exeception.BusinessException;
import com.luyuan.yuanpicturebackend.Exeception.ErrorCode;
import com.luyuan.yuanpicturebackend.annotation.AuthCheck;
import com.luyuan.yuanpicturebackend.common.BaseResponse;
import com.luyuan.yuanpicturebackend.common.ResultUtils;
import com.luyuan.yuanpicturebackend.constant.UserConstant;
import com.luyuan.yuanpicturebackend.manager.CosManager;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.utils.IOUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

@RestController
@RequestMapping("/file")
@Slf4j
public class FileController {

    @Resource
    private CosManager cosManager;

    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/test/upload")
    public BaseResponse<String> testUploadFile(@RequestPart("file")MultipartFile multipartFile) {
        // 文件目录
        String filename = multipartFile.getOriginalFilename();
        String filepath = String.format("/test/%s", filename);
        File file = null;

        try {
            file = File.createTempFile(filepath, null);
            multipartFile.transferTo(file);
            cosManager.putObject(filepath,file);
            // 返回可访问的地址
            return ResultUtils.success(filepath);
        } catch (IOException e) {
            log.error("file upload error, filepath = " + filepath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"上传失败");
        } finally {
            if (file != null){
                // 删除临时文件
                boolean delete = file.delete();
                if (!delete) {
                    log.error("file delete error, filepath = {}", filepath);
                }
            }
        }
    }
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/test/download")
    public void testDownloadFile(String filepath, HttpServletResponse response) throws IOException {
        COSObjectInputStream  cosObjectInputStream = null;

        try {
            COSObject object = cosManager.getObject(filepath);
            cosObjectInputStream = object.getObjectContent();
            // 处理下载的流
            byte[] byteArray = IOUtils.toByteArray(cosObjectInputStream);
            // 设置响应头
            response.getOutputStream().write(byteArray);
            response.getOutputStream().flush();
        } catch (IOException e) {
            log.error("file download error, filepath = " + filepath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"下载失败");
        } finally {
            if (cosObjectInputStream != null){
                cosObjectInputStream.close();
            }
        }
    }
}
