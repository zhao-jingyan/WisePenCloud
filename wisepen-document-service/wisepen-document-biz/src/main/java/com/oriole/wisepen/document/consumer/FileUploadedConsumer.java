package com.oriole.wisepen.document.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oriole.wisepen.document.api.domain.base.DocumentStatus;
import com.oriole.wisepen.document.api.domain.mq.DocumentParseTaskMessage;
import com.oriole.wisepen.document.api.enums.DocumentStatusEnum;
import com.oriole.wisepen.document.domain.entity.DocumentInfoEntity;
import com.oriole.wisepen.document.mq.KafkaDocumentEventPublisher;
import com.oriole.wisepen.document.repository.DocumentInfoRepository;
import com.oriole.wisepen.document.service.IDocumentService;
import com.oriole.wisepen.file.storage.api.domain.mq.FileUploadedMessage;
import com.oriole.wisepen.file.storage.api.enums.StorageSceneEnum;
import com.oriole.wisepen.file.storage.api.feign.RemoteStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.oriole.wisepen.file.storage.api.constant.MqTopicConstants.TOPIC_FILE_UPLOADED;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileUploadedConsumer {

    private final DocumentInfoRepository documentInfoRepository;
    private final IDocumentService documentService;
    private final KafkaDocumentEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = TOPIC_FILE_UPLOADED, groupId = "wisepen-document-upload-callback-group")
    public void onFileUploaded(String payload) throws JsonProcessingException {
        // 从兼容非Java微服务的发布者订阅，使用objectMapper显式转换
        FileUploadedMessage msg = objectMapper.readValue(payload, FileUploadedMessage.class);
        process(msg);
    }

    private void process(FileUploadedMessage msg) {
        if (msg.getScene() != StorageSceneEnum.PRIVATE_DOC || Boolean.TRUE.equals(msg.getFlashUploaded())){
            return; // 不处理非PRIVATE_DOC的上传通知，也不处理秒传的
        }

        DocumentInfoEntity entity = documentInfoRepository.findBySourceObjectKeyOrPreviewObjectKey(msg.getObjectKey()).orElse(null);
        if (entity == null) {
            // 用户已经取消文件处理，删除文档
            eventPublisher.publishFileDeleteEvent(List.of(msg.getObjectKey()));
            log.warn("未找到对应文档，已经删除上传的文件 objectKey={}", msg.getObjectKey());
            return;
        }

        if (DocumentStatusEnum.UPLOADING != entity.getDocumentStatus().getStatus()) {
            log.debug("文档已处理, 跳过 documentId={} status={}", entity.getDocumentId(), entity.getDocumentStatus().getStatus());
            return;
        }

        // 用 OSS 回传的真实 size 覆盖
        entity.getUploadMeta().setSize(msg.getSize());
        documentInfoRepository.save(entity);

        // 推进状态机
        documentService.updateStatus(entity.getDocumentId(), new DocumentStatus(DocumentStatusEnum.UPLOADED));
        eventPublisher.publishParseTask(
                DocumentParseTaskMessage.builder()
                        .documentId(entity.getDocumentId())
                        .sourceObjectKey(entity.getSourceObjectKey())
                        .fileType(entity.getUploadMeta().getFileType())
                        .build()
        );
        log.info("文档上传回调处理完成 documentId={}", entity.getDocumentId());
    }
}
