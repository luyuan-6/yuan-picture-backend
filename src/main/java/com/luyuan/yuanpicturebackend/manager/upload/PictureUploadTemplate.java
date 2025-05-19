package com.luyuan.yuanpicturebackend.manager.upload;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.luyuan.yuanpicturebackend.Exeception.BusinessException;
import com.luyuan.yuanpicturebackend.Exeception.ErrorCode;
import com.luyuan.yuanpicturebackend.config.CosClientConfig;
import com.luyuan.yuanpicturebackend.manager.CosManager;
import com.luyuan.yuanpicturebackend.model.dto.file.UploadPictureResult;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.io.File;
import java.util.Date;
import java.util.Objects;

@Slf4j
public abstract class PictureUploadTemplate {
    @Resource
    protected CosManager cosManager;

    @Resource
    protected CosClientConfig cosClientConfig;

    /**
     * 模板方法，定义上传流程
     */
    public UploadPictureResult uploadPicture(Object inputSource, String uploadPathPrefix) {
        // 1. 校验图片各个元信息
        validPicture(inputSource);

        // 2. 组装拼接图片上传地址
        // 先根据时间戳 + 生成文件名字 + 原始文件后缀
        String uuid = RandomUtil.randomString(16);
        String originalFilename = getOriginFilename(inputSource);
        String uploadFilepath = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid, (Objects.equals(FileUtil.getSuffix(originalFilename), "") ? "jpg" : FileUtil.getSuffix(originalFilename)));
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFilepath);

        File file = null;
        try {
            // 3. 创建临时文件
            file = File.createTempFile(uploadPath, null);
            // 处理文件来源 (本地 或者 URL)
            processFile(inputSource, file);

            // 4. 上传照片到对象存储
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            if (putObjectResult == null || putObjectResult.getETag() == null) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "COS 返回结果异常");
            }

            // 5. 封装返回结果
            return buildResult(putObjectResult, originalFilename, file, uploadPath);

        } catch (Exception e) {
            log.error("图片上传到对象存储失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            // 6.临时文件清理
            this.deleteTempFile(file);
        }
    }

    private UploadPictureResult buildResult(PutObjectResult putObjectResult, String originalFilename, File file, String uploadPath) {
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
    }

    protected abstract void validPicture(Object inputSource);

    /**
     * 获取输入源的原始文件名
     */
    protected abstract String getOriginFilename(Object inputSource);

    /**
     * 处理输入源并生成本地临时文件
     */
    protected abstract void processFile(Object inputSource, File file) throws Exception;

    /**
     * 清理临时文件
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
