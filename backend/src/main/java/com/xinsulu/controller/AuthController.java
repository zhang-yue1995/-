package com.xinsulu.controller;

import com.xinsulu.common.api.ApiResponse;
import com.xinsulu.dto.LoginDTO;
import com.xinsulu.service.AuthService;
import com.xinsulu.vo.UserVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 * 提供登录、登出、用户信息查询等接口
 *
 * @author xinsulu-team
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@Api(tags = "认证管理")
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * 用户登录
     *
     * @param loginDTO 登录信息
     * @return 用户信息和Token
     */
    @PostMapping("/login")
    @ApiOperation(value = "用户登录", notes = "账号密码由部署环境变量配置")
    public ApiResponse<UserVO> login(@RequestBody LoginDTO loginDTO) {
        log.info("收到登录请求：username={}", loginDTO.getUsername());
        UserVO userVO = authService.login(loginDTO);
        return ApiResponse.success(userVO);
    }

    /**
     * 用户登出
     *
     * @param token 令牌（从Header获取）
     * @return 操作结果
     */
    @PostMapping("/logout")
    @ApiOperation(value = "用户登出", notes = "退出当前登录状态")
    public ApiResponse<Void> logout(@RequestHeader(value = "Authorization", required = false) String token) {
        log.info("收到登出请求");
        authService.logout(token);
        return ApiResponse.success();
    }

    /**
     * 获取当前用户信息
     *
     * @param token 令牌
     * @return 用户信息
     */
    @GetMapping("/userinfo")
    @ApiOperation(value = "获取当前用户信息", notes = "根据Token返回当前登录用户详情")
    public ApiResponse<UserVO> getUserInfo(@RequestHeader(value = "Authorization", required = false) String token) {
        log.info("获取用户信息请求");
        UserVO userVO = authService.getCurrentUser(token);
        return ApiResponse.success(userVO);
    }
}
