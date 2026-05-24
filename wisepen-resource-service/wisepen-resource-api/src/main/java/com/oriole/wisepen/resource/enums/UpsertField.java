package com.oriole.wisepen.resource.enums;

/**
 * ES 端 Upsert 时按位选择本次要写入的字段。
 * <p>
 * 通过 {@code EnumSet<UpsertField>} 控制粒度，避免长正文（CONTENT）被零散的元数据更新覆盖，
 * 也避免 ACL 重算事件覆盖正在同步的正文。
 */
public enum UpsertField {

    /** 资源名（resourceName） */
    RESOURCE_NAME,

    /** 正文（content） */
    CONTENT,

    /** ACL 投影字段：ownerId / specifiedDiscoverUsers / computedGroupAcls */
    ACL,

    /** 标签名列表（tags） */
    TAGS;
}
