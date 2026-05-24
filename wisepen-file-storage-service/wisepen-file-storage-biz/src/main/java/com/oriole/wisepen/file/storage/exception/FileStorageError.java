package com.oriole.wisepen.file.storage.exception;

import com.oriole.wisepen.common.core.domain.IResult;
import com.oriole.wisepen.common.core.domain.ResultKey;
import com.oriole.wisepen.common.core.domain.enums.BusinessDomain;
import com.oriole.wisepen.common.core.exception.ErrorReason;
import com.oriole.wisepen.file.storage.api.constant.StorageSubject;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 文件存储微服务(7)专属业务错误
 */
@Getter
@AllArgsConstructor
public enum FileStorageError implements IResult {

    FILE_RECORD_NOT_FOUND(7111,  new ResultKey(BusinessDomain.STORAGE, StorageSubject.FILE, ErrorReason.NOT_FOUND), "文件物理记录不存在"),
    FILE_SIZE_ABOVE_UPPER_BOUND(7121, new ResultKey(BusinessDomain.STORAGE, StorageSubject.FILE, ErrorReason.ABOVE_UPPER_BOUND), "文件大小超过上限"),
    CANNOT_SUPPORT_FILE_TYPE(7131, new ResultKey(BusinessDomain.STORAGE, StorageSubject.FILE, ErrorReason.UNSUPPORTED), "不能处理该文件，文件类型不受支持"),
    CANNOT_SUPPORT_FILE_STORAGE_SCENE(7132, new ResultKey(BusinessDomain.STORAGE, StorageSubject.FILE, ErrorReason.UNSUPPORTED), "不能处理该文件，存储场景不受支持"),
    FILE_URL_INVALID(7141, new ResultKey(BusinessDomain.STORAGE, StorageSubject.FILE, ErrorReason.INVALID),"文件 URL 格式无效"),

    CANNOT_SUPPORT_STORAGE_PROVIDER(7211, new ResultKey(BusinessDomain.STORAGE, StorageSubject.STORAGE_PROVIDER, ErrorReason.UNSUPPORTED),"不能使用该存储供应商，供应商不受支持"),
    STORAGE_PROVIDER_UPLOAD_FILE_FAILED(7221, new ResultKey(BusinessDomain.STORAGE, StorageSubject.STORAGE_PROVIDER, ErrorReason.EXTERNAL_FAILED),"在存储供应商处上传文件失败"),
    STORAGE_PROVIDER_GET_FILE_DOWNLOAD_URL_FAILED(7222, new ResultKey(BusinessDomain.STORAGE, StorageSubject.STORAGE_PROVIDER, ErrorReason.EXTERNAL_FAILED), "在存储供应商处获取文件下载链接失败"),
    STORAGE_PROVIDER_DELETE_FILE_FAILED(7223, new ResultKey(BusinessDomain.STORAGE, StorageSubject.STORAGE_PROVIDER, ErrorReason.EXTERNAL_FAILED), "在存储供应商处删除文件失败"),
    STORAGE_PROVIDER_COPY_FILE_FAILED(7224, new ResultKey(BusinessDomain.STORAGE, StorageSubject.STORAGE_PROVIDER, ErrorReason.EXTERNAL_FAILED),"在存储供应商处复制文件失败"),
    STORAGE_PROVIDER_READ_FILE_FAILED(7225, new ResultKey(BusinessDomain.STORAGE, StorageSubject.STORAGE_PROVIDER, ErrorReason.EXTERNAL_FAILED),"在存储供应商处读取文件失败"),
    STORAGE_PROVIDER_GENERATE_STS_TOKEN_FAILED(7226, new ResultKey(BusinessDomain.STORAGE, StorageSubject.STORAGE_PROVIDER, ErrorReason.EXTERNAL_FAILED),"在存储供应商处生成 STS 临时凭证失败"),
    STORAGE_PROVIDER_GENERATE_CALLBACK_POLICY_FAILED(7231, new ResultKey(BusinessDomain.STORAGE, StorageSubject.STORAGE_PROVIDER, ErrorReason.FAILED),"生成直传回调策略失败"),
    STORAGE_PROVIDER_CALLBACK_SIGNATURE_INVALID(7241, new ResultKey(BusinessDomain.STORAGE, StorageSubject.STORAGE_PROVIDER, ErrorReason.FAILED), "存储供应商的回调签名校验失败");

    private final Integer code;
    private final ResultKey key;
    private final String msg;
}
