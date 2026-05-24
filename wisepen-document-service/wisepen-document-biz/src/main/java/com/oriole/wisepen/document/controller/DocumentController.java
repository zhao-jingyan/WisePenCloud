package com.oriole.wisepen.document.controller;

import com.oriole.wisepen.common.core.context.SecurityContextHolder;
import com.oriole.wisepen.common.core.domain.R;
import com.oriole.wisepen.common.core.exception.ServiceException;
import com.oriole.wisepen.common.security.annotation.CheckLogin;
import com.oriole.wisepen.document.api.domain.base.DocumentInfoBase;
import com.oriole.wisepen.document.api.domain.base.DocumentStatus;
import com.oriole.wisepen.document.api.domain.dto.req.DocumentUploadInitRequest;
import com.oriole.wisepen.document.api.domain.dto.res.DocumentInfoResponse;
import com.oriole.wisepen.document.api.domain.dto.res.DocumentUploadInitResponse;
import com.oriole.wisepen.document.service.IDocumentPreviewService;
import com.oriole.wisepen.document.service.IDocumentService;
import com.oriole.wisepen.resource.domain.dto.ResourceCheckPermissionReqDTO;
import com.oriole.wisepen.resource.domain.dto.ResourceCheckPermissionResDTO;
import com.oriole.wisepen.resource.domain.dto.ResourceInfoGetReqDTO;
import com.oriole.wisepen.resource.domain.dto.res.ResourceItemResponse;
import com.oriole.wisepen.resource.enums.ResourceAccessRole;
import com.oriole.wisepen.resource.enums.ResourceAction;
import com.oriole.wisepen.resource.feign.RemoteResourceService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.oriole.wisepen.document.exception.DocumentError.DOCUMENT_PERMISSION_DENIED;


@Slf4j
@RestController
@RequestMapping("/document")
@RequiredArgsConstructor
@CheckLogin
public class DocumentController {

    private final IDocumentService documentService;
    private final IDocumentPreviewService documentPreviewService;
    private final RemoteResourceService remoteResourceService;

    @Operation(summary = "上传文档（初始化）", description = "用户提供文档信息以换取对象文件存储的PUT URL并使用此上传文件")
    @PostMapping("/uploadDoc")
    public R<DocumentUploadInitResponse> uploadDoc(@Valid @RequestBody DocumentUploadInitRequest request) {
        Long uploaderId = SecurityContextHolder.getUserId();
        return R.ok(documentService.initUploadDocument(request, uploaderId));
    }

    @Operation(summary = "获取未就绪文档列表", description = "用户获取当前未就绪（处理中/失败）的文档列表")
    @GetMapping("/listPendingDocs")
    public R<List<DocumentInfoBase>> listPendingDocs() {
        Long uploaderId = SecurityContextHolder.getUserId();
        return R.ok(documentService.listPendingDocs(uploaderId));
    }

    @Operation(summary = "刷新文档状态", description = "用户刷新文档当前的处理状态")
    @PostMapping("/syncDocStatus")
    public R<DocumentStatus> syncDocStatus(@RequestParam String documentId) {
        documentService.assertDocumentUploader(documentId, SecurityContextHolder.getUserId());
        return R.ok(documentService.refreshDocumentStatus(documentId));
    }

    @Operation(summary = "重试文档处理", description = "用户重试转换失败后的文档，仅在文档处于 FAILED/REGISTERING_RES_TIMEOUT 状态时可调用")
    @PostMapping("/retryDocProcess")
    public R<Void> retryDocProcess(@RequestParam String documentId) {
        documentService.assertDocumentUploader(documentId, SecurityContextHolder.getUserId());
        documentService.retryDocProcess(documentId);
        return R.ok();
    }

    @Operation(summary = "终止文档处理", description = "用户终止/取消未就绪的文档处理，在文档处理的任意阶段均可调用：上传中（取消上传）、转换中（异步退出）")
    @PostMapping("/cancelDocProcess")
    public R<Void> cancelDocProcess(@RequestParam String documentId) {
        documentService.assertDocumentUploader(documentId, SecurityContextHolder.getUserId());
        documentService.deletedDocument(documentId);
        return R.ok();
    }

    @Operation(summary = "获取文档预览")
    @GetMapping("/getDocPreview")
    public void previewDocument(@RequestParam String resourceId,
                                HttpServletRequest request,
                                HttpServletResponse response) {
        String userId = String.valueOf(SecurityContextHolder.getUserId());
        ResourceCheckPermissionResDTO permission = remoteResourceService.checkResPermission(new ResourceCheckPermissionReqDTO(
                resourceId, SecurityContextHolder.getUserId(), SecurityContextHolder.getGroupRoleMap()
        )).getData();
        if (permission.getResourceAccessRole() == ResourceAccessRole.OWNER || permission.getAllowedActions().contains(ResourceAction.VIEW)) {
            documentPreviewService.handlePreviewRequest(request, response, resourceId, userId);
        } else {
            throw new ServiceException(DOCUMENT_PERMISSION_DENIED);
        }
    }

    @Operation(summary = "获取文档信息")
    @GetMapping("/getDocInfo")
    public R<DocumentInfoResponse> getNoteInfo(@RequestParam String resourceId) {
        // 若无权限将抛出异常，此处无需重复鉴权
        ResourceItemResponse resourceInfo = remoteResourceService.getResourceInfo(new ResourceInfoGetReqDTO(
                resourceId, SecurityContextHolder.getUserId(), SecurityContextHolder.getGroupRoleMap()
        )).getData();
        DocumentInfoBase documentInfo = documentService.getDocumentInfo(resourceId);
        DocumentInfoResponse documentInfoResponse = DocumentInfoResponse.builder().resourceInfo(resourceInfo).documentInfo(documentInfo).build();
        return R.ok(documentInfoResponse);
    }
}
