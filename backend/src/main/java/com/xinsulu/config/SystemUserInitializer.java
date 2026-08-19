package com.xinsulu.config;

import com.xinsulu.entity.User;
import com.xinsulu.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 确保配置中允许登录的系统账号具有对应数据库记录。
 * 密码由部署环境变量校验，数据库仅保存不可用于登录的占位值。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SystemUserInitializer implements ApplicationRunner {

    private static final String EXTERNAL_PASSWORD_PLACEHOLDER = "{ENVIRONMENT_MANAGED}";

    private final UserRepository userRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        ensureUser("admin", "系统管理员", "ADMIN", "admin@xinsulu.com", "13800138000");
        ensureUser("manager", "客户经理", "MANAGER", "manager@xinsulu.com", "13800138001");
    }

    private void ensureUser(String username, String realName, String role, String email, String phone) {
        if (userRepository.findByUsername(username).isPresent()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        User user = new User();
        user.setUsername(username);
        user.setPassword(EXTERNAL_PASSWORD_PLACEHOLDER);
        user.setRealName(realName);
        user.setEmail(email);
        user.setPhone(phone);
        user.setRole(role);
        user.setStatus(1);
        user.setCreatedTime(now);
        user.setUpdatedTime(now);
        user.setDeleted(0);
        userRepository.save(user);
        log.info("系统账号初始化完成：username={}", username);
    }
}
