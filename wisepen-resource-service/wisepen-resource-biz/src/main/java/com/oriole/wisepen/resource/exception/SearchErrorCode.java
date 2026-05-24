package com.oriole.wisepen.resource.exception;

import com.oriole.wisepen.common.core.domain.IResult;
import com.oriole.wisepen.common.core.domain.ResultKey;
import com.oriole.wisepen.common.core.domain.enums.BusinessDomain;
import com.oriole.wisepen.common.core.exception.ErrorReason;
import com.oriole.wisepen.resource.constant.ResourceSubject;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 资源微服务 - 搜索域专属错误码，号段 51000–51999，与资源域 {@link ResourceError} 号段隔离。
 */
@Getter
@AllArgsConstructor
public enum SearchErrorCode implements IResult {

    /** ES 集群连接异常 */
    ES_CONNECTION_ERROR(51001, new ResultKey(BusinessDomain.RESOURCE, ResourceSubject.RESOURCE, ErrorReason.EXTERNAL_UNAVAILABLE), "搜索引擎连接异常"),
    /** 搜索查询条件构建失败 */
    ES_QUERY_BUILD_ERROR(51002, new ResultKey(BusinessDomain.RESOURCE, ResourceSubject.RESOURCE, ErrorReason.INTERNAL_ERROR), "搜索查询条件构建失败"),
    /** Kafka 同步消息解析失败 */
    ES_SYNC_MESSAGE_ERROR(51003, new ResultKey(BusinessDomain.RESOURCE, ResourceSubject.RESOURCE, ErrorReason.INTERNAL_ERROR), "搜索同步消息解析失败");

    private final Integer code;
    private final ResultKey key;
    private final String msg;
}
