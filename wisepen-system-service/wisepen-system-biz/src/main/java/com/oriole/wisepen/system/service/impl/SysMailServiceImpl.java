package com.oriole.wisepen.system.service.impl;

import com.oriole.wisepen.common.core.exception.ServiceException;
import com.oriole.wisepen.system.api.domain.dto.MailSendDTO;
import com.oriole.wisepen.system.excpetion.SysError;
import com.oriole.wisepen.system.service.SysMailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 邮件发送服务实现类
 *
 * @author Heng.Xiong, Jiazheng.Sun
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysMailServiceImpl implements SysMailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${spring.mail.from.name:WisePen系统}")
    private String fromName;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendMail(MailSendDTO mailSendDTO) {
        try {

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(mailSendDTO.getToEmail());
            helper.setSubject(mailSendDTO.getSubject());
            helper.setText(mailSendDTO.getContent(), true);

            mailSender.send(message);
            log.info("mail sent. toEmail={} subject={}",
                    mailSendDTO.getToEmail(), mailSendDTO.getSubject());
        } catch (Exception e) {
            log.error("mail send failed. toEmail={} subject={}",
                    mailSendDTO.getToEmail(), mailSendDTO.getSubject(), e);
            throw new ServiceException(SysError.MAIL_SEND_FAILED);
        }
    }
}
