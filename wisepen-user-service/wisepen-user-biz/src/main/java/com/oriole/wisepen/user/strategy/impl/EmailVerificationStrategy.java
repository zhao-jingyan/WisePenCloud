package com.oriole.wisepen.user.strategy.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.oriole.wisepen.common.core.exception.ServiceException;
import com.oriole.wisepen.system.api.domain.dto.MailSendDTO;
import com.oriole.wisepen.system.api.feign.RemoteMailService;
import com.oriole.wisepen.user.api.config.UserProperties;
import com.oriole.wisepen.user.api.domain.dto.VerificationResultDTO;
import com.oriole.wisepen.user.api.enums.Status;
import com.oriole.wisepen.user.api.enums.UserVerificationMode;
import com.oriole.wisepen.user.cache.RedisCacheManager;
import com.oriole.wisepen.user.domain.entity.UserEntity;
import com.oriole.wisepen.user.exception.UserError;
import com.oriole.wisepen.user.mapper.UserMapper;
import com.oriole.wisepen.user.strategy.UserVerificationStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailVerificationStrategy implements UserVerificationStrategy {

    private final RedisCacheManager redisCacheManager;
    private final RemoteMailService remoteMailService;
    private final UserMapper userMapper;
    private final UserProperties userProperties;

    private final TemplateEngine templateEngine;

    @Override
    public UserVerificationMode getMode() {
        return UserVerificationMode.EDU_EMAIL; // 策略标识
    }

    @Override
    public void initiate(Long userId, Map<String, Object> payload) {
        String email = (String)  payload.get("email");

        if (StrUtil.isBlank(email) || !email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.edu(\\.cn)?$")) {
            log.warn("email verification skipped. email={} reason=\"invalid edu email\"", email);
            throw new ServiceException(UserError.VERIFICATION_EMAIL_INVALID);
        }

        long existed = userMapper.selectCount(Wrappers.<UserEntity>lambdaQuery()
                .eq(UserEntity::getEmail, email)
                .eq(UserEntity::getStatus, Status.NORMAL)
                .ne(UserEntity::getUserId, userId));

        if (existed > 0) {
            log.warn("email verification skipped. email={} userId={} reason=\"email already bound\"", email, userId);
            throw new ServiceException(UserError.VERIFICATION_EMAIL_ALREADY_EXISTS);
        }

        String token = redisCacheManager.setEmailVerificationCode(email, userId);

        // 构建重置链接
        String resetLink = userProperties.getApiDomain() + "/verify-email?token=" + token;

        // 构建重置邮件
        Context context = new Context();
        context.setVariable("reset_link", resetLink);
        context.setVariable("current_date", DateUtil.now());
        // Thymeleaf 渲染
        String emailContent = templateEngine.process("verfiyMailTemplate", context);

        MailSendDTO mailDTO = MailSendDTO.builder().toEmail(email).subject("WisePen 邮箱验证").content(emailContent).build();

        try {
            remoteMailService.sendMail(mailDTO);
            log.info("email verification mail sent. userId={} email={}", userId, email);
        } catch (Exception e) {
            log.error("email verification mail send failed. userId={} email={}", userId, email, e);
            throw new ServiceException(UserError.VERIFICATION_EMAIL_SEND_FAILED);
        }
    }

    @Override
    public VerificationResultDTO verify(Map<String, Object> payload) {
        ImmutablePair<Long, String> verfiyInfo = redisCacheManager.getEmailVerificationUser((String) payload.get("token"));
        if (verfiyInfo == null) {
            throw new ServiceException(UserError.VERIFICATION_EMAIL_TOKEN_EXPIRED);
        }
        Long userId = verfiyInfo.left;
        String email = verfiyInfo.right;

        // 在最终更新状态前，再次检查邮箱唯一性
        long existed = userMapper.selectCount(Wrappers.<UserEntity>lambdaQuery()
                .eq(UserEntity::getEmail, email)
                .eq(UserEntity::getStatus, Status.NORMAL)
                .ne(UserEntity::getUserId, userId));

        if (existed > 0) {
            log.warn("email verification skipped. email={} userId={} reason=\"verified by other user\"", email, userId);
            throw new ServiceException(UserError.VERIFICATION_EMAIL_ALREADY_EXISTS);
        }

        UserEntity userEntity = new UserEntity();
        userEntity.setUserId(userId);
        userEntity.setEmail(email);
        userEntity.setStatus(Status.NORMAL);
        userEntity.setVerificationMode(UserVerificationMode.EDU_EMAIL);

        userMapper.updateById(userEntity);
        return VerificationResultDTO.success();
    }

    @Override
    public List<String> getReadonlyFields() {
        return Arrays.asList("username", "email", "status");
    }
}
