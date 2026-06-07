package com.oriole.wisepen.document.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;


@Tag(name = "内部 - 文档", description = "供业务微服务调用的文档内部接口")
@RestController
@RequestMapping("/internal/document")
@RequiredArgsConstructor
public class InternalDocumentController {

}
