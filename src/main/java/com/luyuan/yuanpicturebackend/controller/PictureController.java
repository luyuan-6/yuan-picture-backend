package com.luyuan.yuanpicturebackend.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.luyuan.yuanpicturebackend.Exeception.BusinessException;
import com.luyuan.yuanpicturebackend.Exeception.ErrorCode;
import com.luyuan.yuanpicturebackend.Exeception.ThrowUtils;
import com.luyuan.yuanpicturebackend.annotation.AuthCheck;
import com.luyuan.yuanpicturebackend.api.ImageSearchApiFacade;
import com.luyuan.yuanpicturebackend.api.model.ImageSearchResult;
import com.luyuan.yuanpicturebackend.common.BaseResponse;
import com.luyuan.yuanpicturebackend.common.DeleteRequest;
import com.luyuan.yuanpicturebackend.common.ResultUtils;
import com.luyuan.yuanpicturebackend.constant.UserConstant;
import com.luyuan.yuanpicturebackend.model.dto.picture.*;
import com.luyuan.yuanpicturebackend.model.entity.Picture;
import com.luyuan.yuanpicturebackend.model.entity.Space;
import com.luyuan.yuanpicturebackend.model.entity.User;
import com.luyuan.yuanpicturebackend.model.enums.PictureReviewStatusEnum;
import com.luyuan.yuanpicturebackend.model.vo.PictureTagCategory;
import com.luyuan.yuanpicturebackend.model.vo.PictureVO;
import com.luyuan.yuanpicturebackend.service.PictureService;
import com.luyuan.yuanpicturebackend.service.SpaceService;
import com.luyuan.yuanpicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/picture")
@Slf4j
public class PictureController {

    private final Cache<String, String> LOCAL_CACHE = Caffeine.newBuilder()
            //  缓存 1 万条
            .maximumSize(10000L)
            // 缓存5分钟
            .expireAfterWrite(5L, TimeUnit.MINUTES).build();
    @Resource
    private UserService userService;
    @Resource
    private PictureService pictureService;
    @Resource
    private SpaceService spaceService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 上传图片（可重新上传）
     */
    @PostMapping("/upload")
    public BaseResponse<PictureVO> uploadPicture(@RequestPart("file") MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 通过 URL 上传图片（可重新上传）
     */
    @PostMapping("/upload/url")
    public BaseResponse<PictureVO> uploadPictureByUrl(@RequestBody PictureUploadRequest pictureUploadRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        String fileUrl = pictureUploadRequest.getFileUrl();
        PictureVO pictureVO = pictureService.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 删除图片
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        pictureService.deletePicture(deleteRequest.getId(), loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 跟新图片
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/update")
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateRequest pictureUpdateRequest, HttpServletRequest request) {
        if (pictureUpdateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = pictureUpdateRequest.getId();
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 判断是否存在
        Picture oldPicture = pictureService.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 将实体类和DTO类进行转换
        Picture picture = new Picture();
        BeanUtil.copyProperties(pictureUpdateRequest, picture);
        // 注意 list 转为 string
        picture.setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));
        // 校验图片元信息
        pictureService.validPicture(picture);
        // 补充审核参数
        User loginUser = userService.getLoginUser(request);
        pictureService.fillReviewParams(picture, loginUser);
        // 操作数据库
        boolean result = pictureService.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.PARAMS_ERROR, "更新图片失败");
        // 清理对象存储里的文件
        if (!oldPicture.getUrl().equals(picture.getUrl())) {
            pictureService.clearPictureFile(oldPicture);
        }
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取图片（仅管理员可用）
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Picture> getPictureById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        // 空间权限校验
        Long spaceId = picture.getSpaceId();
        if (spaceId != null) {
            User user = userService.getLoginUser(request);
            pictureService.checkPictureAuth(user, picture);
        }
        // 获取封装类
        return ResultUtils.success(picture);
    }

    /**
     * 根据 id 获取图片（封装类）
     */
    @GetMapping("/get/vo")
    public BaseResponse<PictureVO> getPictureVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        return ResultUtils.success(pictureService.getPictureVO(picture, request));
    }

    /**
     * 分页获取图片列表(管理员可用)
     *
     * @param pictureQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Picture>> listPictureByPage(@RequestBody PictureQueryRequest pictureQueryRequest) {
        int current = pictureQueryRequest.getCurrent();
        int pageSize = pictureQueryRequest.getPageSize();
        // 1.创建分页实例对象
        Page<Picture> picture = new Page<>(current, pageSize);
        // 2.创建查询条件对象
        QueryWrapper<Picture> queryWrapper = pictureService.getQueryWrapper(pictureQueryRequest);
        // 3.查询数据库
        Page<Picture> picturePage = pictureService.page(picture, queryWrapper);
        return ResultUtils.success(picturePage);
    }

    /**
     * 分页获取图片列表（封装类）
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<PictureVO>> listPictureVOByPage(@RequestBody PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
        int current = pictureQueryRequest.getCurrent();
        int pageSize = pictureQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR, "每页条数不能超过20");
        // 1.创建分页实例对象
        Page<Picture> picture = new Page<>(current, pageSize);
        // 2.创建查询条件对象
//        // 普通用户默认只能查看已过审的数据
//        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
        // 空间权限校验
        Long spaceId = pictureQueryRequest.getSpaceId();
        if (spaceId == null) {
            // 普通用户默认只能查看已过审的数据
            pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            pictureQueryRequest.setNullSpaceId(true);
        } else {
            // 私有空间
            User loginUser = userService.getLoginUser(request);
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            if (!loginUser.getId().equals(space.getUserId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有空间权限");
            }
        }
        QueryWrapper<Picture> queryWrapper = pictureService.getQueryWrapper(pictureQueryRequest);
        // 3.查询数据库
        Page<Picture> picturePage = pictureService.page(picture, queryWrapper);
        return ResultUtils.success(pictureService.getPictureVOPage(picturePage, request));
    }

    /**
     * 分页获取图片列表（封装类 带缓存）
     */
    @PostMapping("/list/page/vo/cache")
    public BaseResponse<Page<PictureVO>> listPictureVOByPageWithCache(@RequestBody PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
        int current = pictureQueryRequest.getCurrent();
        int pageSize = pictureQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR, "每页条数不能超过20");
        // 1.创建分页实例对象
        Page<Picture> picture = new Page<>(current, pageSize);
        // 2.创建查询条件对象
        // 普通用户默认只能查看已过审的数据
        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
        QueryWrapper<Picture> queryWrapper = pictureService.getQueryWrapper(pictureQueryRequest);

        // 构建缓存
        String queryCondition = JSONUtil.toJsonStr(pictureQueryRequest);
        String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
        String cacheKey = "listPictureVOByPageWithCache:" + hashKey;
        // 1. 从本地缓存中获取缓存
        String cachedValue = LOCAL_CACHE.getIfPresent(cacheKey);
        if (cachedValue != null) {
            // 如果缓存命中，返回结果
            Page<PictureVO> cachedPage = JSONUtil.toBean(cachedValue, Page.class);
            return ResultUtils.success(cachedPage);
        }

        // 2. 查询分布式缓存（Redis）
        ValueOperations<String, String> stringStringValueOperations = stringRedisTemplate.opsForValue();
        cachedValue = stringStringValueOperations.get(cacheKey);
        if (cachedValue != null) {
            // 如果缓存命中，返回结果
            Page<PictureVO> cachedPage = JSONUtil.toBean(cachedValue, Page.class);
            return ResultUtils.success(cachedPage);
        }

        // 3.查询数据库
        Page<Picture> picturePage = pictureService.page(picture, queryWrapper);
        // 获取封装类
        Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(picturePage, request);
        // 4. 更新缓存
        String cacheValue = JSONUtil.toJsonStr(pictureVOPage);

        // 写入本地缓存
        LOCAL_CACHE.put(cacheKey, cacheValue);

        // 写入 Redis 缓存
        // 5 - 10 分钟随机过期 防止雪崩
        int expireTime = RandomUtil.randomInt(5, 10) * 60;
        stringStringValueOperations.set(cacheKey, cacheValue, expireTime, TimeUnit.SECONDS);

        return ResultUtils.success(pictureVOPage);
    }

    @PostMapping("/edit")
    public BaseResponse<Boolean> editPicture(@RequestBody PictureEditRequest pictureEditRequest, HttpServletRequest request) {
        if (pictureEditRequest == null || pictureEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        pictureService.editPicture(pictureEditRequest, loginUser);
        return ResultUtils.success(true);
    }

    @GetMapping("/tag_category")
    public BaseResponse<PictureTagCategory> listPictureTagCategory() {
        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        List<String> tagList = Arrays.asList("热门", "搞笑", "生活", "高清", "艺术", "校园", "背景", "简历", "创意");
        List<String> categoryList = Arrays.asList("模版", "电商", "表情包", "素材", "海报");
        pictureTagCategory.setTagList(tagList);
        pictureTagCategory.setCategoryList(categoryList);
        return ResultUtils.success(pictureTagCategory);
    }

    /**
     * 审核图片
     */
    @PostMapping("/review")
    public BaseResponse<Boolean> doPictureReview(@RequestBody PictureReviewRequest pictureReviewRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        pictureService.doPictureReview(pictureReviewRequest, loginUser);
        return ResultUtils.success(true);
    }


    /**
     * 批量上传
     */
    @PostMapping("/upload/batch")
    public BaseResponse<Integer> uploadPictureByBatch(@RequestBody PictureUploadByBatchRequest pictureUploadByBatchRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(pictureUploadByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        Integer uploadCount = pictureService.uploadPictureByBatch(pictureUploadByBatchRequest, loginUser);
        return ResultUtils.success(uploadCount);
    }

    /**
     * 以图搜图
     */
    @PostMapping("/search/picture")
    public BaseResponse<List<ImageSearchResult>> searchPictureByPicture(@RequestBody SearchPictureByPictureRequest searchPictureByPictureRequest) {
        ThrowUtils.throwIf(searchPictureByPictureRequest == null, ErrorCode.PARAMS_ERROR);
        Long pictureId = searchPictureByPictureRequest.getPictureId();
        ThrowUtils.throwIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR);
        Picture oldPicture = pictureService.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 防止百度搜索图片时图片格式不支持
        String url = oldPicture.getUrl() + "?imageMogr2/format/png";
        List<ImageSearchResult> resultList = ImageSearchApiFacade.searchImage(url);
        return ResultUtils.success(resultList);
    }

    /**
     * 颜色搜图
     */
    @PostMapping("/search/color")
    public BaseResponse<List<PictureVO>> searchPictureByColor(@RequestBody SearchPictureByColorRequest searchPictureByColorRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(searchPictureByColorRequest == null, ErrorCode.PARAMS_ERROR);
        Long spaceId = searchPictureByColorRequest.getSpaceId();
        ThrowUtils.throwIf(spaceId == null || spaceId <= 0, ErrorCode.PARAMS_ERROR);
        String picColor = searchPictureByColorRequest.getPicColor();
        ThrowUtils.throwIf(picColor == null , ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        List<PictureVO> resultList = pictureService.searchPictureByColor(spaceId, picColor, loginUser);

        return ResultUtils.success(resultList);
    }


}
