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
@RestController
@RequestMapping("/system/mail")
@RequiredArgsConstructor
@Tag(name = "邮件服务", description = "系统邮件发送")
public class MailController {

    private final SysMailService sysMailService;

    /**
     * 通用邮件发送
     */
    @PostMapping("/send")
    @Operation(summary = "发送邮件", operationId = "sendMail")
    public R<Void> sendMail(@RequestBody MailSendDTO mailSendDTO) {
        sysMailService.sendMail(mailSendDTO);
        return R.ok();
    }
}
