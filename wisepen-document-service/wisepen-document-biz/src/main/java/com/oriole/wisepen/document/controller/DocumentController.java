package com.oriole.wisepen.document.controller;

import com.oriole.wisepen.common.core.context.SecurityContextHolder;
import com.oriole.wisepen.common.core.domain.R;
import com.oriole.wisepen.common.core.domain.enums.BusinessType;
import com.oriole.wisepen.common.core.exception.ServiceException;
import com.oriole.wisepen.common.log.annotation.Log;
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
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.oriole.wisepen.document.exception.DocumentError.DOCUMENT_PERMISSION_DENIED;


@Slf4j
@Tag(name = "文档处理", description = "文档上传、处理状态、预览与信息查询")
@RestController
@RequestMapping("/document")
@RequiredArgsConstructor
@CheckLogin
public class DocumentController {

    private final IDocumentService documentService;
    private final IDocumentPreviewService documentPreviewService;
    private final RemoteResourceService remoteResourceService;

    @Operation(
            summary = "初始化文档上传",
            description = """
                    - 用途：为当前用户创建文档上传任务，并申请对象存储直传凭证。
                    - 请求：filename 为展示文件名；extension 为文件扩展名；md5 用于秒传判定；expectedSize 为预期文件大小。
                    - 约束：当前用户必须已登录；扩展名必须属于文档服务支持的文件类型；请求字段必须通过校验。
                    - 处理：生成 documentId，向文件存储服务申请上传 URL 或触发秒传；保存文档上传元信息。命中秒传时立即发布文档解析任务；未命中秒传时不直接接收文件内容。
                    - 失败：未登录 -> PermissionError.NOT_LOGIN；文件类型不支持 -> DocumentError.CANNOT_SUPPORT_FILE_TYPE；存储服务申请上传凭证失败 -> DocumentError.DOCUMENT_UPLOAD_URL_APPLY_FAILED。
                    - 响应：返回 documentId、objectKey、上传凭证信息和是否秒传。
                    """
    )
    @Log(title = "上传文档", businessType = BusinessType.INSERT)
    @PostMapping("/uploadDoc")
    public R<DocumentUploadInitResponse> uploadDoc(@Valid @RequestBody DocumentUploadInitRequest request) {
        Long uploaderId = SecurityContextHolder.getUserId();
        return R.ok(documentService.initUploadDocument(request, uploaderId));
    }

    @Operation(
            summary = "查询未就绪文档",
            description = """
                    - 用途：查询当前用户仍处于上传、转换、解析或失败状态的文档任务。
                    - 请求：无显式请求参数，上传者来自当前登录上下文。
                    - 约束：当前用户必须已登录。
                    - 处理：按当前用户筛选 UPLOADING、UPLOADED、TRANSFER_TIMEOUT、CONVERTING_AND_PARSING 和 FAILED 状态的文档；不刷新存储状态，也不触发重试。
                    - 失败：未登录 -> PermissionError.NOT_LOGIN。
                    - 响应：返回未就绪文档基础信息列表。
                    """
    )
    @GetMapping("/listPendingDocs")
    public R<List<DocumentInfoBase>> listPendingDocs() {
        Long uploaderId = SecurityContextHolder.getUserId();
        return R.ok(documentService.listPendingDocs(uploaderId));
    }

    @Operation(
            summary = "刷新文档状态",
            description = """
                    - 用途：主动同步当前用户上传文档的最新处理状态。
                    - 请求：documentId 指定待刷新的文档任务。
                    - 约束：当前用户必须是该文档上传者；目标文档必须存在。
                    - 处理：如果文档仍处于 UPLOADING，会查询文件存储记录；确认上传完成后更新为 UPLOADED 并发布解析任务。非 UPLOADING 状态仅返回当前状态，不重复推进处理。
                    - 失败：未登录 -> PermissionError.NOT_LOGIN；文档不存在 -> DocumentError.DOCUMENT_NOT_FOUND；当前用户不是上传者 -> DocumentError.DOCUMENT_PERMISSION_DENIED；存储状态查询失败 -> DocumentError.DOCUMENT_STORAGE_STATUS_GET_FAILED。
                    - 响应：返回刷新后的文档状态。
                    """
    )
    @PostMapping("/syncDocStatus")
    public R<DocumentStatus> syncDocStatus(@RequestParam String documentId) {
        documentService.assertDocumentUploader(documentId, SecurityContextHolder.getUserId());
        return R.ok(documentService.refreshDocumentStatus(documentId));
    }

    @Operation(
            summary = "重试文档处理",
            description = """
                    - 用途：让上传者重新推进失败或资源注册超时的文档处理任务。
                    - 请求：documentId 指定待重试文档。
                    - 约束：当前用户必须是该文档上传者；文档状态只能是 FAILED 或 REGISTERING_RES_TIMEOUT。
                    - 处理：FAILED 状态会重置为 UPLOADED 并重新发布解析任务；REGISTERING_RES_TIMEOUT 状态会重新执行资源注册完成流程；不重新申请上传 URL，不重新上传源文件。
                    - 失败：未登录 -> PermissionError.NOT_LOGIN；文档不存在 -> DocumentError.DOCUMENT_NOT_FOUND；当前用户不是上传者 -> DocumentError.DOCUMENT_PERMISSION_DENIED；文档状态不允许重试 -> DocumentError.CANNOT_RETRY_DOCUMENT_PROCESS_IN_CURRENT_STATE；资源注册失败 -> DocumentError.DOCUMENT_REGISTER_RESOURCE_FAILED。
                    - 响应：成功时返回空结果。
                    """
    )
    @PostMapping("/retryDocProcess")
    public R<Void> retryDocProcess(@RequestParam String documentId) {
        documentService.assertDocumentUploader(documentId, SecurityContextHolder.getUserId());
        documentService.retryDocProcess(documentId);
        return R.ok();
    }

    @Operation(
            summary = "取消文档处理",
            description = """
                    - 用途：上传者取消未完成的文档上传或处理任务。
                    - 请求：documentId 指定待取消文档。
                    - 约束：当前用户必须是该文档上传者；READY 状态文档不能取消；CONVERTING_AND_PARSING 状态当前不允许取消。
                    - 处理：删除文档信息、解析内容和 PDF 元数据记录，并发布相关对象存储文件删除事件；不删除已经注册为资源且处于 READY 的文档。
                    - 失败：未登录 -> PermissionError.NOT_LOGIN；文档不存在 -> DocumentError.DOCUMENT_NOT_FOUND；当前用户不是上传者 -> DocumentError.DOCUMENT_PERMISSION_DENIED；READY 文档不允许取消 -> DocumentError.CANNOT_CANCEL_READY_DOCUMENT_PROCESS；当前状态不允许取消 -> DocumentError.CANNOT_CANCEL_DOCUMENT_PROCESS_IN_CURRENT_STATE。
                    - 响应：成功时返回空结果。
                    """
    )
    @PostMapping("/cancelDocProcess")
    public R<Void> cancelDocProcess(@RequestParam String documentId) {
        documentService.assertDocumentUploader(documentId, SecurityContextHolder.getUserId());
        documentService.deletedDocument(documentId);
        return R.ok();
    }

    @Operation(
            summary = "获取文档预览",
            description = """
                    - 用途：为有查看权限的用户输出文档 PDF 预览流。
                    - 请求：resourceId 指定文档资源；Range 请求头可用于分段读取。
                    - 约束：当前用户必须已登录，且必须是资源所有者或拥有 VIEW 动作；文档必须已经处理完成并具备预览文件。
                    - 处理：先通过资源服务校验权限，再读取文档预览元数据和对象存储下载地址；支持全量或 Range 响应，并在预览流尾部追加水印附录；不修改文档内容或资源权限。
                    - 失败：未登录 -> PermissionError.NOT_LOGIN；资源无查看权限 -> DocumentError.DOCUMENT_PERMISSION_DENIED；文档不存在 -> DocumentError.DOCUMENT_NOT_FOUND；预览未就绪 -> DocumentError.DOCUMENT_PREVIEW_NOT_READY；预览元数据缺失或响应流写入失败 -> DocumentError.DOCUMENT_PREVIEW_FAILED。
                    - 响应：直接写出 application/pdf 预览流。
                    """
    )
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

    @Operation(
            summary = "获取文档信息",
            description = """
                    - 用途：获取文档资源详情和文档处理信息，用于文档详情页展示。
                    - 请求：resourceId 指定文档资源。
                    - 约束：当前用户必须已登录，且必须通过资源服务的资源详情权限校验；目标文档信息必须存在。
                    - 处理：通过资源服务获取资源详情和当前用户可执行动作，再读取文档信息并组合响应；不刷新文档状态，不触发解析或重试。
                    - 失败：未登录 -> PermissionError.NOT_LOGIN；资源不存在 -> ResourceError.RESOURCE_NOT_FOUND；资源无查看权限 -> ResourceError.RESOURCE_PERMISSION_DENIED；文档不存在 -> DocumentError.DOCUMENT_NOT_FOUND。
                    - 响应：返回资源信息与文档信息组合结果。
                    """
    )
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
