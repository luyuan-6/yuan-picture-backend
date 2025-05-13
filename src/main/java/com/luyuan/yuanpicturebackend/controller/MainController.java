package com.luyuan.yuanpicturebackend.controller;

import com.luyuan.yuanpicturebackend.common.BaseResponse;
import com.luyuan.yuanpicturebackend.common.ResultUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
//@CrossOrigin(origins = {"http://localhost:8081"}, allowCredentials = "true")
public class MainController {

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public BaseResponse<String> health() {
        return ResultUtils.success("ok");
    }

}
