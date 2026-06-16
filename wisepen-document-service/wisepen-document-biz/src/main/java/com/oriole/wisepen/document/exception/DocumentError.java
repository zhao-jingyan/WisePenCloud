package com.oriole.wisepen.document.exception;

import com.oriole.wisepen.common.core.domain.IResult;
import com.oriole.wisepen.common.core.domain.ResultKey;
import com.oriole.wisepen.common.core.domain.enums.BusinessDomain;
import com.oriole.wisepen.common.core.exception.ErrorReason;
import com.oriole.wisepen.document.api.constant.DocumentSubject;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 文档微服务(6)专属业务错误
 */
@Getter
@AllArgsConstructor
public enum DocumentError implements IResult {

    // 文档相关异常
    DOCUMENT_NOT_FOUND(6111, new ResultKey(BusinessDomain.DOCUMENT, DocumentSubject.DOCUMENT, ErrorReason.NOT_FOUND),"文档不存在"),
    DOCUMENT_PERMISSION_DENIED(6121, new ResultKey(BusinessDomain.DOCUMENT, DocumentSubject.DOCUMENT, ErrorReason.PERMISSION_DENIED),"无权访问或操作该文档"),
    CANNOT_SUPPORT_FILE_TYPE(6131, new ResultKey(BusinessDomain.DOCUMENT, DocumentSubject.DOCUMENT, ErrorReason.UNSUPPORTED),"不能处理该文件，文件类型不受支持"),
    DOCUMENT_UPLOAD_URL_APPLY_FAILED(6141, new ResultKey(BusinessDomain.DOCUMENT, DocumentSubject.DOCUMENT, ErrorReason.FAILED),"申请文档上传 URL 失败"),
    DOCUMENT_STORAGE_STATUS_GET_FAILED(6142, new ResultKey(BusinessDomain.DOCUMENT, DocumentSubject.DOCUMENT, ErrorReason.FAILED),"获取文档存储文件状态失败"),
    DOCUMENT_REGISTER_RESOURCE_FAILED(6143, new ResultKey(BusinessDomain.DOCUMENT, DocumentSubject.DOCUMENT, ErrorReason.FAILED),"注册文档资源失败"),
    DOCUMENT_FORK_FAILED(6144, new ResultKey(BusinessDomain.DOCUMENT, DocumentSubject.DOCUMENT, ErrorReason.FAILED),"文档复制失败"),

    // 文档预览相关异常
    DOCUMENT_PREVIEW_NOT_READY(6211, new ResultKey(BusinessDomain.DOCUMENT, DocumentSubject.DOCUMENT_PREVIEW, ErrorReason.STATE_INVALID),"文档尚未就绪，不能预览"),
    DOCUMENT_PREVIEW_META_NOT_FOUND(6221, new ResultKey(BusinessDomain.DOCUMENT, DocumentSubject.DOCUMENT_PREVIEW, ErrorReason.NOT_FOUND),"文档PDF META信息不存在或已损坏"),
    DOCUMENT_PREVIEW_FAILED(6231, new ResultKey(BusinessDomain.DOCUMENT, DocumentSubject.DOCUMENT_PREVIEW, ErrorReason.FAILED),"文档预览失败"),

    // 文档处理相关异常
    CANNOT_CANCEL_READY_DOCUMENT_PROCESS(6311, new ResultKey(BusinessDomain.DOCUMENT, DocumentSubject.DOCUMENT_PROCESS, ErrorReason.STATE_INVALID),"文档已就绪，不能取消处理流程"),
    CANNOT_CANCEL_DOCUMENT_PROCESS_IN_CURRENT_STATE(6312, new ResultKey(BusinessDomain.DOCUMENT, DocumentSubject.DOCUMENT_PROCESS, ErrorReason.STATE_INVALID),"文档当前状态不能取消处理流程"),
    CANNOT_RETRY_DOCUMENT_PROCESS_IN_CURRENT_STATE(6313, new ResultKey(BusinessDomain.DOCUMENT, DocumentSubject.DOCUMENT_PROCESS, ErrorReason.STATE_INVALID),"文档当前状态不能重试处理流程"),
    DOCUMENT_PROCESS_CONVERT_FAILED(6321, new ResultKey(BusinessDomain.DOCUMENT, DocumentSubject.DOCUMENT_PROCESS, ErrorReason.FAILED),"文档 PDF 转换失败"),
    DOCUMENT_PROCESS_CONTENT_READ_FAILED(6322, new ResultKey(BusinessDomain.DOCUMENT, DocumentSubject.DOCUMENT_PROCESS, ErrorReason.FAILED),"文档文本内容读取失败");


    private final Integer code;
    private final ResultKey key;
    private final String msg;
}
