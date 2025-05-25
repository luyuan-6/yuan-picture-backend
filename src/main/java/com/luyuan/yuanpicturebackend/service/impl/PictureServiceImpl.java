package com.luyuan.yuanpicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.luyuan.yuanpicturebackend.Exeception.BusinessException;
import com.luyuan.yuanpicturebackend.Exeception.ErrorCode;
import com.luyuan.yuanpicturebackend.Exeception.ThrowUtils;
import com.luyuan.yuanpicturebackend.manager.CosManager;
import com.luyuan.yuanpicturebackend.manager.upload.FilePictureUpload;
import com.luyuan.yuanpicturebackend.manager.upload.PictureUploadTemplate;
import com.luyuan.yuanpicturebackend.manager.upload.UrlPictureUpload;
import com.luyuan.yuanpicturebackend.mapper.PictureMapper;
import com.luyuan.yuanpicturebackend.model.dto.file.UploadPictureResult;
import com.luyuan.yuanpicturebackend.model.dto.picture.*;
import com.luyuan.yuanpicturebackend.model.entity.Picture;
import com.luyuan.yuanpicturebackend.model.entity.Space;
import com.luyuan.yuanpicturebackend.model.entity.User;
import com.luyuan.yuanpicturebackend.model.enums.PictureReviewStatusEnum;
import com.luyuan.yuanpicturebackend.model.vo.PictureVO;
import com.luyuan.yuanpicturebackend.model.vo.UserVO;
import com.luyuan.yuanpicturebackend.service.PictureService;
import com.luyuan.yuanpicturebackend.service.SpaceService;
import com.luyuan.yuanpicturebackend.service.UserService;
import com.luyuan.yuanpicturebackend.utils.ColorSimilarUtils;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.awt.*;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author luyuan
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @createDate 2025-05-14 17:45:39
 */
@Service
@Slf4j
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture> implements PictureService {

    @Resource
    private UserService userService;

    @Resource
    private FilePictureUpload filePictureUpload;

    @Resource
    private UrlPictureUpload urlPictureUpload;

    @Resource
    private SpaceService spaceService;

    @Resource
    private CosManager cosManager;

    @Resource
    private TransactionTemplate transactionTemplate;


    @Override
    public void validPicture(Picture picture) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR, "图片为空");
        // 从对象中取值进行校验
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
        // 修改数据时 id 不能为空 有参数则校验
        ThrowUtils.throwIf(ObjectUtil.isNull(id), ErrorCode.PARAMS_ERROR, "图片 id 不能为空");
        if (CharSequenceUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url过长");
        }
        if (CharSequenceUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");
        }
    }

    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        if (inputSource == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片为空");
        }

        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);

        // 校验空间是否存在
        Long spaceId = pictureUploadRequest.getSpaceId();
        if (spaceId != null) {
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            // 必须是空间创建人(管理员)才能上传
            if (!space.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有该空间上传权限");
            }
            // 校验额度
            if (space.getTotalCount() >= space.getMaxCount()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间上传图片数量超过限制");
            }
            if (space.getTotalSize() >= space.getMaxSize()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间大小不足");
            }
        }
        // 用于判断是新增还是更新图片
        Long pictureId = null;
        if (pictureUploadRequest.getId() != null) {
            pictureId = pictureUploadRequest.getId();
        }
        // 如果是更新图片，需要校验图片是否存在
//        if (pictureId != null) {
//            boolean exists = this.lambdaQuery()
//                    .eq(Picture::getId, pictureId)
//                    .exists();
//            ThrowUtils.throwIf(!exists, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
//        }
        // 如果是更新图片，需要校验图片是否存在
        if (pictureId != null) {
            Picture oldPicture = this.getById(pictureId);
            ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
            // 仅本人或管理员可编辑
            if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
            // 校验空间是否一致
            if (spaceId == null) {
                // 没传spaceId，使用原有图片空间
                if (oldPicture.getSpaceId() != null) {
                    spaceId = oldPicture.getSpaceId();
                }
            } else {
                // 传了 spaceId 必须和原有图片一致
                if (!oldPicture.getSpaceId().equals(spaceId)) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间不一致");
                }
            }
        }

        // 上传图片，得到图片信息
        // 按照用户 id 划分目录
        String uploadPathPrefix = null;
        if (spaceId == null) {
            uploadPathPrefix = String.format("public/%s", loginUser.getId());
        } else {
            uploadPathPrefix = String.format("space/%s", spaceId);
        }
        // 根据 inputSource 类型区分上传类型
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        if (inputSource instanceof String) {
            pictureUploadTemplate = urlPictureUpload;
        }
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);

        // 构造要入库的图片信息
        Picture picture = new Picture();
        picture.setSpaceId(spaceId);
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());
        String picName = uploadPictureResult.getPicName();
        if (CharSequenceUtil.isNotBlank(pictureUploadRequest.getPicName())) {
            picName = pictureUploadRequest.getPicName();
        }
        if (CharSequenceUtil.isNotBlank(pictureUploadRequest.getCategory())) {
            picture.setCategory(pictureUploadRequest.getCategory());
        }
        if (CharSequenceUtil.isNotBlank(pictureUploadRequest.getTags())) {
            picture.setTags(pictureUploadRequest.getTags());
        }
        picture.setName(picName);
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setPicColor(uploadPictureResult.getPicColor());
        picture.setUserId(loginUser.getId());

        // 补充审核参数
        fillReviewParams(picture, loginUser);
        // 如果是更新，需要补充 id 和编辑时间
        if (pictureId != null) {
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }
        Long finalSpaceId = spaceId;
        transactionTemplate.execute(status -> {
            boolean result = this.saveOrUpdate(picture);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片上传失败");
            if (finalSpaceId != null) {
                boolean update = spaceService.lambdaUpdate().eq(Space::getId, finalSpaceId).setSql("totalSize = totalSize + " + picture.getPicSize()).setSql("totalCount = totalCount + 1").update();
                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败");
            }
            return true;
        });
//        boolean result = this.saveOrUpdate(picture);
//        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片上传失败");
        return PictureVO.objToVo(picture);
    }

    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (pictureQueryRequest == null) {
            return queryWrapper;
        }
        // 从查询请求中获取查询参数
        Long id = pictureQueryRequest.getId();
        Long spaceId = pictureQueryRequest.getSpaceId();
        boolean nullSpaceId = pictureQueryRequest.isNullSpaceId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        String searchText = pictureQueryRequest.getSearchText();
        Long userId = pictureQueryRequest.getUserId();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();
        Date endEditTime = pictureQueryRequest.getEndEditTime();
        Date startEditTime = pictureQueryRequest.getStartEditTime();
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Long reviewerId = pictureQueryRequest.getReviewerId();

        // 拼接查询条件
        if (CharSequenceUtil.isNotBlank(searchText)) {
            // 需要拼接查询条件
            queryWrapper.and(qw -> qw.like("name", searchText)).or().like("introduction", searchText);
        }
        queryWrapper.eq(ObjectUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjectUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjectUtil.isNotEmpty(spaceId), "spaceId", spaceId);
        queryWrapper.isNull(nullSpaceId, "spaceId");
        queryWrapper.like(CharSequenceUtil.isNotBlank(name), "name", name);
        queryWrapper.like(CharSequenceUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.like(CharSequenceUtil.isNotBlank(picFormat), "picFormat", picFormat);
        queryWrapper.eq(CharSequenceUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjectUtil.isNotEmpty(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjectUtil.isNotEmpty(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjectUtil.isNotEmpty(picSize), "picSize", picSize);
        queryWrapper.eq(ObjectUtil.isNotEmpty(picScale), "picScale", picScale);
        queryWrapper.eq(ObjectUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
        queryWrapper.like(CharSequenceUtil.isNotBlank(reviewMessage), "reviewMessage", reviewMessage);
        queryWrapper.eq(ObjectUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId);
        // 区间 : startEditTime<= and  < endEditTime
        queryWrapper.ge(ObjectUtil.isNotEmpty(startEditTime), "editTime", startEditTime);
        queryWrapper.lt(ObjectUtil.isNotEmpty(endEditTime), "editTime", endEditTime);
        // JSON 数组查询
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        // 排序
        queryWrapper.orderBy(CharSequenceUtil.isNotBlank(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    @Override
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {
        // 对象封装类
        PictureVO pictureVO = PictureVO.objToVo(picture);
        // 关联查询用户信息
        Long userId = picture.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            pictureVO.setUser(userVO);
        }
        return pictureVO;
    }

    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {
        List<Picture> pictureList = picturePage.getRecords();
        long current = picturePage.getCurrent();
        long total = picturePage.getTotal();
        long size = picturePage.getSize();
        Page<PictureVO> pictureVOPage = new Page<>(current, size, total);
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }
        // 对象列表 =》 封装对象列表
        List<PictureVO> pictureVOList = pictureList.stream().map(PictureVO::objToVo).collect(Collectors.toList());
        // 1. 关联查询的用户信息
        Set<Long> userIdSet = pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream().collect(Collectors.groupingBy(User::getId));
        // 2. 填充用户信息信息
        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            pictureVO.setUser(userService.getUserVO(user));
        });
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }

    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        Long id = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        String reviewMessage = pictureReviewRequest.getReviewMessage();
        PictureReviewStatusEnum enumByValue = PictureReviewStatusEnum.getEnumByValue(reviewStatus);
        if (id == null || enumByValue == null || PictureReviewStatusEnum.REVIEWING.equals(enumByValue)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 判断图片是否存在
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 已是该状态
        if (oldPicture.getReviewStatus().equals(reviewStatus)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请勿重复审批");
        }
        // 更新审核状态
        Picture updatePicture = new Picture();
        BeanUtil.copyProperties(pictureReviewRequest, updatePicture);
        updatePicture.setReviewerId(loginUser.getId());
        updatePicture.setReviewTime(new Date());
        boolean result = this.updateById(updatePicture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    @Override
    public void fillReviewParams(Picture picture, User loginUser) {
        if (userService.isAdmin(loginUser)) {
            // 管理员自动过审
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewerId(loginUser.getId());
            picture.setReviewMessage("管理员自动过审");
            picture.setReviewTime(new Date());
        } else {
            // 非管理员，创建或编辑都要改为待审核
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }
    }

    @Override
    public Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
        String searchText = pictureUploadByBatchRequest.getSearchText();
        Integer count = pictureUploadByBatchRequest.getCount();
        if (count <= 0 || count > 30) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数count错误");
        }
        // 数据源地址
        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", searchText);
        Document document = null;
        try {
            document = Jsoup.connect(fetchUrl).get();
        } catch (IOException e) {
            log.info("Jsoup 获取图片失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Jsoup 获取图片失败");
        }
        Element div = document.getElementsByClass("dgControl").first();
        if (ObjectUtil.isNull(div)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取 document 元素失败");
        }
        Elements imgElementList = div.select("img.mimg");
        int uploadSuccessCount = 0;
        for (Element imgElement : imgElementList) {
            String imgUrl = imgElement.attr("src");
            if (CharSequenceUtil.isBlank(imgUrl)) {
                log.info("图片地址为空 已跳过: {}", imgUrl);
                continue;
            }
            // 处理图片上传地址 防止出现转移问题
            int questionMarkIndex = imgUrl.indexOf("?");
            if (questionMarkIndex > -1) {
                imgUrl = imgUrl.substring(0, questionMarkIndex);
            }
            // 上传图片
            PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();

            if (CharSequenceUtil.isNotBlank(pictureUploadByBatchRequest.getNamePrefix())) {
                pictureUploadRequest.setPicName(pictureUploadByBatchRequest.getNamePrefix() + "_" + uploadSuccessCount + 1);
            }
            if (CharSequenceUtil.isNotBlank(pictureUploadByBatchRequest.getCategory())) {
                pictureUploadRequest.setCategory(pictureUploadByBatchRequest.getCategory());
            }
            if (CharSequenceUtil.isNotBlank(JSONUtil.toJsonStr(pictureUploadByBatchRequest.getTags()))) {
                pictureUploadRequest.setTags(JSONUtil.toJsonStr(pictureUploadByBatchRequest.getTags()));
            }

            try {
                PictureVO pictureVO = this.uploadPicture(imgUrl, pictureUploadRequest, loginUser);
                log.info("图片上传成功: id =  {}", pictureVO.getId());
                uploadSuccessCount++;
            } catch (Exception e) {
                log.error("图片上传失败: {}", imgUrl);
                continue;
            }
            // 当前上传的图片 exceeds the maximum number
            if (uploadSuccessCount >= count) {
                break;
            }

        }
        return uploadSuccessCount;
    }

    @Override
    @Async
    public void clearPictureFile(Picture oldPicture) {
        if (oldPicture == null) {
            log.error("[clearPictureFile] oldPicture is null");
            return;
        }
        // 判断该图片是否被多条记录使用 分片上传才会用到这个逻辑 目前没有使用分块上传
        String url = oldPicture.getUrl();
        Long count = this.lambdaQuery().eq(Picture::getUrl, url).count();
        // 有不只一条记录用到了该图片 不清理
        if (count > 1) {
            return;
        }
        // 注意，这里的 url 包含了域名，实际上只要传 key 值（存储路径）就够了
        try {
            // 提取路径部分
            // 清理原图
            String originPicPath = new URL(url).getPath();
            if (originPicPath != null) {
                cosManager.deleteObject(originPicPath);
            }

            // 清理缩略图
            String thumbnailUrl = oldPicture.getThumbnailUrl();
            if (thumbnailUrl != null) {
                String thumbnailPicPath = new URL(thumbnailUrl).getPath();
                cosManager.deleteObject(thumbnailPicPath);
            }

        } catch (MalformedURLException e) {
            log.error("处理图片删除时，格式错误。 图片URL : {}", url);
        }

    }

    @Override
    public void checkPictureAuth(User loginUser, Picture picture) {
        Long spaceId = picture.getSpaceId();
        if (spaceId == null) {
            // 公共图库，仅本人或者管理员可编辑
            if (!userService.isAdmin(loginUser) && !loginUser.getId().equals(picture.getUserId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        } else {
            // 私有空间，仅空间管理员可操作
            if (!picture.getUserId().equals(loginUser.getId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }
    }

    @Override
    public void deletePicture(long pictureId, User loginUser) {
        ThrowUtils.throwIf(pictureId <= 0, ErrorCode.PARAMS_ERROR);
        // 查询图片是否存在
        Picture oldPicture = this.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅限本人或管理员可以删除
        if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 校验权限
        checkPictureAuth(loginUser, oldPicture);
        // 操作数据库
//        boolean result = this.removeById(pictureId);
//        ThrowUtils.throwIf(!result, ErrorCode.PARAMS_ERROR, "删除图片失败");
        transactionTemplate.execute(status -> {
            boolean result = this.removeById(pictureId);
            ThrowUtils.throwIf(!result, ErrorCode.PARAMS_ERROR, "删除图片失败");
            Long spaceId = oldPicture.getSpaceId();
            if (spaceId != null) {
                boolean deleteResult = spaceService.lambdaUpdate().eq(Space::getId, spaceId).setSql("totalSize = totalSize - " + oldPicture.getPicSize()).setSql("totalCount = totalCount - 1").update();
                ThrowUtils.throwIf(!deleteResult, ErrorCode.OPERATION_ERROR, "额度更新失败");
            }
            return true;
        });
        // 清理对象存储里的文件
        clearPictureFile(oldPicture);

    }

    @Override
    public void editPicture(PictureEditRequest pictureEditRequest, User loginUser) {
        // 实体类和DTO类进行转换
        Picture picture = new Picture();
        BeanUtil.copyProperties(pictureEditRequest, picture);
        // 转换tags list
        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
        // 设置编辑时间
        picture.setEditTime(new Date());
        // 数据校验
        this.validPicture(picture);
        // 查库校验图片是否存在
        Long id = pictureEditRequest.getId();
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
//        // 仅本人或者管理员可编辑
//        User loginUser = userService.getLoginUser(request);
//        if (!oldPicture.getUserId().equals(picture.getUserId()) && !userService.isAdmin(loginUser)) {
//            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
//        }
        // 校验权限
        this.checkPictureAuth(loginUser, oldPicture);
        // 补充审核参数
        this.fillReviewParams(picture, loginUser);
        // 操作数据库对图片进行编辑
        boolean result = this.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    @Override
    public List<PictureVO> searchPictureByColor(Long spaceId, String picColor, User loginUser) {
        // 1. 校验参数是否合法
        ThrowUtils.throwIf(spaceId == null || spaceId <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(picColor == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        // 2. 校验空间权限
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
        // 3. 查询该空间下所有图片(必须有主色调)
        List<Picture> pictureList = this.lambdaQuery()
                .eq(Picture::getSpaceId, spaceId)
                .isNotNull(Picture::getPicColor).list();
        // 如果没有图片，直接返回空列表
        if (pictureList.isEmpty()) {
            return Collections.emptyList();
        }
        // 将目标颜色转为 Color 对象
        Color targetColor = Color.decode(picColor);
        //4. 计算相似度并排序
        pictureList.stream().sorted(Comparator.comparingDouble(picture -> {
            // 提取图片主色调
            String hexColor = picture.getPicColor();
            if (CharSequenceUtil.isBlank(hexColor)) {
                return Double.MAX_VALUE;
            }
            Color pictureColor = Color.decode(hexColor);
            // 越大越相似 负号取反
            return -ColorSimilarUtils.calculateSimilarity(pictureColor, targetColor);
        })).limit(12).collect(Collectors.toList());
        // 转换为脱敏的 VO
        return pictureList.stream().map(PictureVO::objToVo).collect(Collectors.toList());
    }

}




