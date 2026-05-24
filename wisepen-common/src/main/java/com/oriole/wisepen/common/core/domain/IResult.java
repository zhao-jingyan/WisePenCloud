package com.oriole.wisepen.common.core.domain;

public interface IResult {
    Integer getCode();
    ResultKey getKey();
    String getMsg();
}