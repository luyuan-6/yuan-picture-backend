package com.luyuan.yuanpicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

@Data
public class PictureUploadRequest implements Serializable {

    /**
     * 图片 id（用于修改）
     */
    private Long id;

    /**
     * 文件地址
     */
    private String fileUrl;

    /**
     * 图片名称
     */
    private String picName;

    /**
     * 分类
     */
    private String category;

    /**
     * 空间id
     */
    private Long spaceId;

    /**
     * 标签（JSON 数组）
     */
    private String tags;

    private static final long serialVersionUID = 1L;
}
