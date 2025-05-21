package com.luyuan.yuanpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.luyuan.yuanpicturebackend.model.dto.space.SpaceAddRequest;
import com.luyuan.yuanpicturebackend.model.dto.space.SpaceQueryRequest;
import com.luyuan.yuanpicturebackend.model.entity.Space;
import com.luyuan.yuanpicturebackend.model.entity.User;
import com.luyuan.yuanpicturebackend.model.vo.SpaceVO;

import javax.servlet.http.HttpServletRequest;

/**
* @author lhd666
* @description 针对表【space(空间)】的数据库操作Service
* @createDate 2025-05-21 20:29:57
*/
public interface SpaceService extends IService<Space> {

    /**
     * 校验数据
     *
     * @param space
     */
    void validSpace(Space space, boolean add);

    /**
     * 根据空间等级填充空间信息
     *
     * @param space
     */
    void fillSpaceBySpaceLevel(Space space);

    /**
     * 增加空间
     *
     * @param spaceAddRequest
     * @param loginUser
     * @return
     */
    long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);
    /**
     * 获取查询对象
     *
     * @param spaceQueryRequest
     * @return
     */
    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);

    /**
     * 获取图片包装类（单条）
     *
     * @param space
     * @param request
     * @return
     */
    SpaceVO getSpaceVO(Space space, HttpServletRequest request);

    /**
     * 获取图片包装类（分页）
     *
     * @param spacePage
     * @param request
     * @return
     */
    Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request);

}
