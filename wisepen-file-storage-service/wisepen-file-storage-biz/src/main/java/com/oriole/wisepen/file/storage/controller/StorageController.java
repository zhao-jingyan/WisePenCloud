package com.oriole.wisepen.file.storage.controller;

import cn.hutool.core.io.FileUtil;
import com.oriole.wisepen.common.core.domain.R;
import com.oriole.wisepen.common.core.exception.ServiceException;
import com.oriole.wisepen.common.security.annotation.CheckLogin;
import com.oriole.wisepen.file.storage.api.domain.dto.StorageRecordDTO;
import com.oriole.wisepen.file.storage.api.enums.StorageSceneEnum;
import com.oriole.wisepen.file.storage.exception.FileStorageError;
import com.oriole.wisepen.file.storage.service.IStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.EnumSet;

import static com.oriole.wisepen.file.storage.api.enums.StorageSceneEnum.*;

@RestController
@RequestMapping("/storage/")
@RequiredArgsConstructor
@CheckLogin
@Tag(name = "文件存储服务", description = "面向前端的文件与图片上传接口")
public class StorageController {

    private final IStorageService storageService;

    /**
     * 图床代理上传
     * @param file    图片文件
     * @param scene   业务场景
     * @param bizTag  业务隔离标识
     */
    @PostMapping("/imageUpload")
    @Operation(summary = "图床代理上传", operationId = "uploadImageProxy")
    public R<StorageRecordDTO> uploadImageProxy(
            @Parameter(description = "图片文件") @RequestParam("file") MultipartFile file,
            @Parameter(description = "存储场景") @RequestParam(value = "scene", defaultValue = "PUBLIC_IMAGE_FOR_USER") StorageSceneEnum scene,
            @Parameter(description = "业务隔离标识") @RequestParam(value = "bizTag", required = false) String bizTag) {

        String extension = FileUtil.extName(file.getOriginalFilename()).toLowerCase();
        if (!Arrays.asList("jpg", "jpeg", "png", "gif", "webp").contains(extension)) {
            throw new ServiceException(FileStorageError.CANNOT_SUPPORT_FILE_TYPE);
        }
        if (!EnumSet.of(PUBLIC_IMAGE_FOR_USER, PUBLIC_IMAGE_FOR_GROUP, PRIVATE_IMAGE_FOR_NOTE).contains(scene)) {
            throw new ServiceException(FileStorageError.CANNOT_SUPPORT_FILE_STORAGE_SCENE);
        }
        StorageRecordDTO record = storageService.uploadSmallFileProxy(file, scene, bizTag);
        return R.ok(record);
    }
}
