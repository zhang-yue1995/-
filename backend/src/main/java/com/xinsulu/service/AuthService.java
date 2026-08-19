package com.xinsulu.service;

import com.xinsulu.dto.LoginDTO;
import com.xinsulu.vo.UserVO;

/**
 * 认证服务接口
 *
 * @author xinsulu-team
 */
public interface AuthService {

    /**
     * 用户登录
     *
     * @param loginDTO 登录信息
     * @return 用户信息和Token
     */
    UserVO login(LoginDTO loginDTO);

    /**
     * 用户登出
     *
     * @param token 令牌
     */
    void logout(String token);

    /**
     * 获取当前用户信息
     *
     * @param token 令牌
     * @return 用户信息
     */
    UserVO getCurrentUser(String token);

}
