package com.oriole.wisepen.note.exception;

import com.oriole.wisepen.common.core.domain.IResult;
import com.oriole.wisepen.common.core.domain.ResultKey;
import com.oriole.wisepen.common.core.domain.enums.BusinessDomain;
import com.oriole.wisepen.common.core.exception.ErrorReason;
import com.oriole.wisepen.note.api.constant.NoteSubject;
import com.oriole.wisepen.resource.constant.ResourceSubject;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 笔记微服务(8)专属业务错误
 */
@Getter
@AllArgsConstructor
public enum NoteError implements IResult {

    // 笔记相关异常
    NOTE_NOT_FOUND(8111, new ResultKey(BusinessDomain.NOTE, NoteSubject.NOTE, ErrorReason.NOT_FOUND),"资源不存在"),
    NOTE_PERMISSION_DENIED(8121, new ResultKey(BusinessDomain.NOTE, NoteSubject.NOTE, ErrorReason.PERMISSION_DENIED),"无权访问或操作该资源");


    private final Integer code;
    private final ResultKey key;
    private final String msg;
}
