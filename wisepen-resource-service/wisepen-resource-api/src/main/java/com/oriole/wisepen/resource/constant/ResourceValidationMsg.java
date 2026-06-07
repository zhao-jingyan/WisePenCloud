package com.oriole.wisepen.resource.constant;

public interface ResourceValidationMsg {
    // 基础资源属性校验
    String RESOURCE_ID_NOT_BLANK = "资源ID不能为空";
    String RESOURCE_NAME_NOT_BLANK = "资源名称不能为空";
    String RESOURCE_NEW_NAME_NOT_BLANK = "资源新名称不能为空";
    String RESOURCE_TYPE_NOT_NULL = "资源类型不能为空";
    String OWNER_ID_NOT_BLANK = "资源所有者ID不能为空";
    String USER_ID_NOT_NULL = "用户ID不能为空";
    String USER_GROUP_ROLES_NOT_NULL = "用户组-角色表不能为null";

    // 拓扑与组织关系校验
    String GROUP_ID_NOT_BLANK = "组ID不能为空";
    String TAG_IDS_NOT_NULL = "标签列表不能为null";

    String TAG_ID_NOT_BLANK = "目标标签ID不能为空";
    String TAG_NAME_NOT_BLANK = "标签名称不能为空";
    String VISIBILITY_ALL_USERS_EMPTY = "可见性为ALL时，不能指定用户列表";

    // 分页与查询校验 (预留)
    String PAGE_MIN_INVALID = "页码不能小于1";
    String SIZE_MIN_INVALID = "每页条数不能小于1";

    String FILE_ORG_LOGIC_NOT_NULL = "资源组织模式不能为空";

    // 互动相关校验
    String SCORE_RANGE_INVALID = "评分必须在1到5之间";

    // Market 相关校验
    String MARKET_LISTING_ID_NOT_BLANK = "上架记录ID不能为空";
    String MARKET_PURCHASE_ID_NOT_BLANK = "购买记录ID不能为空";
    String MARKET_PRICE_NOT_NULL = "价格不能为空";
    String MARKET_PRICE_INVALID = "价格必须大于0";
    String MARKET_VERSION_NOT_NULL = "上架版本不能为空";
    String MARKET_VERSION_INVALID = "上架版本不能小于0";
    String MARKET_SELL_METHOD_NOT_NULL = "售卖方式不能为空";
}
