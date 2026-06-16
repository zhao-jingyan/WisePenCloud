package com.oriole.wisepen.document.service;

import com.oriole.wisepen.document.api.domain.base.DocumentInfoBase;
import com.oriole.wisepen.document.api.domain.base.DocumentStatus;
import com.oriole.wisepen.document.api.domain.dto.req.DocumentUploadInitRequest;
import com.oriole.wisepen.document.api.domain.dto.res.DocumentUploadInitResponse;
import com.oriole.wisepen.document.domain.entity.DocumentContentEntity;
import com.oriole.wisepen.document.domain.entity.DocumentPdfMetaEntity;
import com.oriole.wisepen.resource.domain.mq.ResourceForkMessage;

import java.util.List;
import java.util.Optional;

public interface IDocumentService {

    // 断言文档的所属权 (仅对未就绪文档有效)
    void assertDocumentUploader(String documentId, Long uploaderId);

    // 初始化上传
    DocumentUploadInitResponse initUploadDocument(DocumentUploadInitRequest request, Long uploaderId);

    // 获取未就绪文档列表
    List<DocumentInfoBase> listPendingDocs(Long uploaderId);

    // 获取文档状态
    Optional<DocumentStatus> getDocumentStatus(String documentId);

    // 刷新/获取文档状态
    DocumentStatus refreshDocumentStatus(String documentId);

    // 重试文档处理
    void retryDocProcess(String documentId);

    // 终止未就绪的文档处理
    void deletedDocument(String documentId);

    // 获取文档信息
    DocumentInfoBase getDocumentInfo(String resourceId);

    // 批量删除文档
    void deleteDocuments(List<String> resourceIds);

    // 更新文档状态
    void updateStatus(String documentId, DocumentStatus status);

    // 归档文档解析的结果
    void saveConversionAndParseResult(String documentId, String previewObjectKey, DocumentPdfMetaEntity meta, DocumentContentEntity content);

    // 文档就绪
    void finalizeToReady(String documentId);

    // 复制文档
    void forkDocument(ResourceForkMessage msg);
}
