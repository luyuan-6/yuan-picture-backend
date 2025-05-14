package com.luyuan.yuanpicturebackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.luyuan.yuanpicturebackend.model.dto.picture.PictureUploadRequest;
import com.luyuan.yuanpicturebackend.model.entity.Picture;
import com.luyuan.yuanpicturebackend.model.entity.User;
import com.luyuan.yuanpicturebackend.model.vo.PictureVO;
import org.springframework.web.multipart.MultipartFile;

/**
* @author lhd666
* @description 针对表【picture(图片)】的数据库操作Service
* @createDate 2025-05-14 17:45:39
*/
public interface PictureService extends IService<Picture> {

    PictureVO uploadPicture(MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest, User loginUser);
}
