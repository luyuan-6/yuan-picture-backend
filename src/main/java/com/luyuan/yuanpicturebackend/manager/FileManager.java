package com.luyuan.yuanpicturebackend.manager;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.luyuan.yuanpicturebackend.Exeception.BusinessException;
import com.luyuan.yuanpicturebackend.Exeception.ErrorCode;
import com.luyuan.yuanpicturebackend.Exeception.ThrowUtils;
import com.luyuan.yuanpicturebackend.config.CosClientConfig;
import com.luyuan.yuanpicturebackend.model.dto.file.UploadPictureResult;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 文件服务
 * @deprecated 已废弃，改为使用 upload 包的模板方法优化
 */
@Deprecated
@Slf4j
@Service
public class FileManager {

    @Resource
    private CosManager cosManager;

    @Resource
    private CosClientConfig cosClientConfig;

    /**
     * 上传图片
     *
     * @param multipartFile
     * @param uploadPathPrefix
     * @return
     */
    public UploadPictureResult uploadPicture(MultipartFile multipartFile, String uploadPathPrefix) {
        // 校验图片各个元信息
        validPicture(multipartFile);
        // 组装拼接图片上传地址
        // 先根据时间戳 + 生成文件名字 + 原始文件后缀
        String uuid = RandomUtil.randomString(16);
        String originalFilename = multipartFile.getOriginalFilename();
        String uploadFilepath = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid, FileUtil.getSuffix(originalFilename));
//        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid,
//                FileUtil.getSuffix(originalFilename));
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFilepath);
        File file = null;

        try {
            // 创建临时文件
            file = File.createTempFile(uploadPath, null);
            multipartFile.transferTo(file);
            // 上传照片
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            // 封装返回结果
            UploadPictureResult uploadPictureResult = new UploadPictureResult();
            int picHeight = imageInfo.getHeight();
            int picWidth = imageInfo.getWidth();
            double picScale = NumberUtil.round(picHeight * 1.0 / picWidth, 2).doubleValue();
            uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
            uploadPictureResult.setPicWidth(picWidth);
            uploadPictureResult.setPicHeight(picHeight);
            uploadPictureResult.setPicScale(picScale);
            uploadPictureResult.setPicFormat(imageInfo.getFormat());
            uploadPictureResult.setPicSize(FileUtil.size(file));
            uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
            return uploadPictureResult;
        } catch (IOException e) {
            log.error("图片上传到对象存储失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            // 临时文件清理
            this.deleteTempFile(file);
        }
    }

    /**
     * 通过 URL 上传图片
     *
     * @param fileUrl
     * @param uploadPathPrefix
     * @return
     */
    public UploadPictureResult uploadUrlPicture(String fileUrl, String uploadPathPrefix) {
        // 校验图片各个元信息
//        validPicture(multipartFile);
        validPicture(fileUrl);
        // 组装拼接图片上传地址
        // 先根据时间戳 + 生成文件名字 + 原始文件后缀
        String uuid = RandomUtil.randomString(16);
//        String originalFilename = multipartFile.getOriginalFilename();
        String originFilename = FileUtil.mainName(fileUrl);
        String uploadFilepath = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid, FileUtil.getSuffix(originFilename));
//        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid,
//                FileUtil.getSuffix(originalFilename));
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFilepath);
        File file = null;

        try {
            // 创建临时文件
            file = File.createTempFile(uploadPath, null);
            // multipartFile.transferTo(file);
            HttpUtil.downloadFile(fileUrl, file);
            // 上传照片
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            // 封装返回结果
            UploadPictureResult uploadPictureResult = new UploadPictureResult();
            int picHeight = imageInfo.getHeight();
            int picWidth = imageInfo.getWidth();
            double picScale = NumberUtil.round(picHeight * 1.0 / picWidth, 2).doubleValue();
            uploadPictureResult.setPicName(originFilename);
            uploadPictureResult.setPicWidth(picWidth);
            uploadPictureResult.setPicHeight(picHeight);
            uploadPictureResult.setPicScale(picScale);
            uploadPictureResult.setPicFormat(imageInfo.getFormat());
            uploadPictureResult.setPicSize(FileUtil.size(file));
            uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
            return uploadPictureResult;
        } catch (IOException e) {
            log.error("图片上传到对象存储失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            // 临时文件清理
            this.deleteTempFile(file);
        }
    }


    /**
     * 校验文件
     *
     * @param multipartFile
     */
    private void validPicture(MultipartFile multipartFile) {
        ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAMS_ERROR, "文件为空");
        // 1. 校验文件大小
        long fileSize = multipartFile.getSize();
        final long OneMB = 1024 * 1024;
        ThrowUtils.throwIf(fileSize > 2 * OneMB, ErrorCode.PARAMS_ERROR, "文件过大");
        // 2. 校验文件后缀
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        // 允许上传的文件后缀列表 (后期可优化)
        List<String> ALLOW_FORMAT_LIST = Arrays.asList("jpg", "jpeg", "png", "webp");
        ThrowUtils.throwIf(!ALLOW_FORMAT_LIST.contains(fileSuffix), ErrorCode.PARAMS_ERROR, "文件格式不支持");

    }


    /**
     * 校验文件
     *
     * @param fileUrl
     */
    private void validPicture(String fileUrl) {
        ThrowUtils.throwIf(StrUtil.isBlank(fileUrl), ErrorCode.PARAMS_ERROR, "文件为空");

        try {
            // 1.校验 URL 格式 验证是否是合法的 URL
            new URL(fileUrl);
        } catch (Exception e) {
            log.error("校验文件失败", e);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件地址格式不正确");
        }

        // 2. 校验 URL 协议 开头
        ThrowUtils.throwIf(!fileUrl.startsWith("http://") && !fileUrl.startsWith("https://"), ErrorCode.PARAMS_ERROR, "文件地址协议不正确");

        // 3. 发送 Head 请求以验证文件是否存在
        HttpResponse response = null;
        try {
            response = HttpUtil.createRequest(Method.HEAD, fileUrl).execute();
            // 为正常返回，无需执行其他判断
            if (response.getStatus() != HttpStatus.HTTP_OK) {
                return;
            }
            // 4. 校验文件类型
            String contentType = response.header("Content-Type");
            if (CharSequenceUtil.isNotBlank(contentType)) {
                // 允许的图片类型
                final List<String> ALLOW_CONTENT_TYPES = Arrays.asList("image/jpeg", "image/jpg", "image/png", "image/webp");
                ThrowUtils.throwIf(!ALLOW_CONTENT_TYPES.contains(contentType), ErrorCode.PARAMS_ERROR, "文件类型不支持");
            }
            // 5. 校验文件大小
            String contentLengthStr  = response.header("Content-Length");
            if (CharSequenceUtil.isNotBlank(contentLengthStr)){
                try {
                    long contentLength = Long.parseLong(contentLengthStr);
                    final long TWO_MB = 2 * 1024 * 1024L; // 限制文件大小为 2MB
                    ThrowUtils.throwIf(contentLength > TWO_MB, ErrorCode.PARAMS_ERROR, "文件大小不能超过 2M");
                } catch (NumberFormatException e) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小格式错误");
                }
            }


        } finally {
            if (response != null){
                response.close();
            }
        }

    }

    /**
     * 清理临时文件
     *
     * @param file
     */
    public void deleteTempFile(File file) {
        if (file == null) {
            return;
        }
        // 删除临时文件
        boolean deleteResult = file.delete();
        if (!deleteResult) {
            log.error("file delete error, filepath = {}", file.getAbsolutePath());
        }
    }

}
