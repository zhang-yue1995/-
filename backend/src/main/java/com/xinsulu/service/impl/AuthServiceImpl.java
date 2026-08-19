package com.xinsulu.service.impl;

import com.xinsulu.common.exception.BusinessException;
import com.xinsulu.dto.LoginDTO;
import com.xinsulu.entity.User;
import com.xinsulu.repository.UserRepository;
import com.xinsulu.service.AuthService;
import com.xinsulu.vo.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 认证服务实现类
 * 管理员与客户经理密码由部署环境变量配置
 *
 * @author xinsulu-team
 */
@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    /** Token存储（生产环境应使用Redis或JWT） */
    private static final Map<String, UserVO> TOKEN_STORE = new ConcurrentHashMap<>();

    private static final Map<String, LocalDateTime> TOKEN_EXPIRES_AT = new ConcurrentHashMap<>();

    @Value("${app.auth.admin-password:admin123}")
    private String adminPassword;

    @Value("${app.auth.manager-password:manager123}")
    private String managerPassword;

    @Value("${app.auth.token-valid-hours:12}")
    private long tokenValidHours;

    @Autowired
    private UserRepository userRepository;

    /**
     * 用户登录验证
     *
     * @param loginDTO 登录信息（用户名、密码）
     * @return 用户信息和Token
     */
    @Override
    @Transactional
    public UserVO login(LoginDTO loginDTO) {
        log.info("用户登录请求：username={}", loginDTO.getUsername());

        // 验证用户名和密码
        Map<String, String> accounts = new HashMap<>();
        accounts.put("admin", adminPassword);
        accounts.put("manager", managerPassword);
        String expectedPassword = accounts.get(loginDTO.getUsername());
        if (expectedPassword == null || !expectedPassword.equals(loginDTO.getPassword())) {
            log.warn("登录失败：用户名或密码错误，username={}", loginDTO.getUsername());
            throw new BusinessException(401, "用户名或密码错误");
        }

        User user = userRepository.findByUsername(loginDTO.getUsername())
                .filter(item -> Integer.valueOf(1).equals(item.getStatus()))
                .filter(item -> Integer.valueOf(0).equals(item.getDeleted()))
                .orElseThrow(() -> new BusinessException(401, "账号不存在或已停用"));

        LocalDateTime loginTime = LocalDateTime.now();
        user.setLastLoginTime(loginTime);
        user.setUpdatedTime(loginTime);
        userRepository.save(user);

        // 使用数据库中的真实用户主键，供上传记录、审计日志等外键字段使用。
        UserVO userVO = new UserVO();
        userVO.setId(user.getId());
        userVO.setUsername(user.getUsername());
        userVO.setRealName(user.getRealName());
        userVO.setRole(user.getRole());
        userVO.setEmail(user.getEmail());
        userVO.setPhone(user.getPhone());
        userVO.setStatus(user.getStatus());
        userVO.setLastLoginTime(loginTime);
        userVO.setCreatedTime(user.getCreatedTime());

        // 生成Token并存储
        String token = UUID.randomUUID().toString().replace("-", "");
        userVO.setToken(token);
        TOKEN_STORE.put(token, userVO);
        TOKEN_EXPIRES_AT.put(token, LocalDateTime.now().plusHours(tokenValidHours));

        log.info("登录成功：username={}, token={}", loginDTO.getUsername(), token.substring(0, 8) + "...");
        return userVO;
    }

    /**
     * 用户登出
     *
     * @param token 令牌
     */
    @Override
    public void logout(String token) {
        token = normalizeToken(token);
        if (token != null && TOKEN_STORE.containsKey(token)) {
            TOKEN_STORE.remove(token);
            TOKEN_EXPIRES_AT.remove(token);
            log.info("用户登出成功");
        }
    }

    /**
     * 获取当前用户信息
     *
     * @param token 令牌
     * @return 用户信息
     */
    @Override
    public UserVO getCurrentUser(String token) {
        token = normalizeToken(token);
        LocalDateTime expiresAt = token == null ? null : TOKEN_EXPIRES_AT.get(token);
        if (token == null || !TOKEN_STORE.containsKey(token)
                || expiresAt == null || !expiresAt.isAfter(LocalDateTime.now())) {
            if (token != null) {
                TOKEN_STORE.remove(token);
                TOKEN_EXPIRES_AT.remove(token);
            }
            throw new BusinessException(401, "Token无效或已过期，请重新登录");
        }
        return TOKEN_STORE.get(token);
    }

    private String normalizeToken(String token) {
        if (token == null) {
            return null;
        }
        String normalized = token.trim();
        if (normalized.regionMatches(true, 0, "Bearer ", 0, 7)) {
            normalized = normalized.substring(7).trim();
        }
        return normalized.isEmpty() ? null : normalized;
    }

}
