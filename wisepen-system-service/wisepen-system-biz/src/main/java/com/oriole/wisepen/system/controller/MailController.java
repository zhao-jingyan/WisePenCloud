package com.oriole.wisepen.system.controller;

import com.oriole.wisepen.common.core.domain.R;
import com.oriole.wisepen.system.api.domain.dto.MailSendDTO;
import com.oriole.wisepen.system.service.SysMailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 邮件发送控制器
 *
 * @author Xiong.Heng
 */
@Tag(name = "内部 - 邮件", description = "供业务微服务发送系统邮件")
@RestController
@RequestMapping("/system/mail")
@RequiredArgsConstructor
public class MailController {

    private final SysMailService sysMailService;

    /**
     * 通用邮件发送
     */
    @Operation(
            summary = "内部发送系统邮件",
            description = """
                    - 用途：供业务服务发送密码重置、验证通知等系统邮件。
                    - 请求：toEmail 为收件人邮箱；subject 为邮件主题；content 为 HTML 邮件正文。
                    - 约束：调用方必须通过内部服务调用边界；邮件配置必须可用；收件人、主题和内容需由上游业务准备。
                    - 处理：使用系统发件地址构造 UTF-8 MIME 邮件并发送；不记录业务发送任务，也不自动重试。
                    - 失败：邮件服务连接失败、地址非法或发送异常 -> SysError.MAIL_SEND_FAILED。
                    - 响应：成功时返回空结果。
                    """
    )
    @PostMapping("/send")
    public R<Void> sendMail(@RequestBody MailSendDTO mailSendDTO) {
        sysMailService.sendMail(mailSendDTO);
        return R.ok();
    }
}
