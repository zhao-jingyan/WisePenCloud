package com.oriole.wisepen.resource.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.oriole.wisepen.common.core.domain.enums.WisePenEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public enum ResourceAction implements WisePenEnum {
    DISCOVER(1 << 0, 1 << 0, "列表可见"),           // 1: 列表可见（发现权）
    VIEW(1 << 1, 1 << 1, "在线阅读"),               // 2: 在线阅读
    EDIT(1 << 2, 1 << 2, "协同编辑"),               // 4: 协同编辑
    DOWNLOAD_WATERMARK(1 << 3, 1 << 3, "下载带水印"), // 8: 导出/下载带水印
    DOWNLOAD_ORIGINAL(1 << 4, 1 << 4, "下载源文件");  // 16: 下载源文件

    public static final int ALL_ACTIONS = (1 << values().length) - 1;
    public static final int DEFAULT_MEMBER_ACTIONS = DISCOVER.code | VIEW.code | DOWNLOAD_WATERMARK.code;

    @EnumValue
    private final Integer code;
    private final Integer value;
    private final String desc;

    public int getImpliedMask() {
        int mask = this.code;
        switch (this) {
            case EDIT:
                // 编辑 必须包含 阅读和可见
                mask |= VIEW.code | DISCOVER.code;
                break;
            case DOWNLOAD_ORIGINAL:
                // 下载源文件 必须包含 带水印下载、阅读和可见
                mask |= DOWNLOAD_WATERMARK.code | VIEW.code | DISCOVER.code;
                break;
            case DOWNLOAD_WATERMARK:
                // 下载带水印 必须包含 阅读和可见
                mask |= VIEW.code | DISCOVER.code;
                break;
            case VIEW:
                // 阅读 必须包含 可见
                mask |= DISCOVER.code;
                break;
            case DISCOVER:
                // 列表可见 是最底层的，不隐含其他权限
            default:
                break;
        }
        return mask;
    }

    // 将权限掩码解析为枚举列表
    public static List<ResourceAction> permissionCodeToActions(int permissionCode) {
        return Arrays.stream(values())
                // 使用按位与(&)判断是否包含该权限
                .filter(action -> (permissionCode & action.code) != 0)
                .collect(Collectors.toList());
    }

    // 将枚举列表合并为一个权限掩码
    public static int actionsToPermissionCode(List<ResourceAction> actions) {
        if (actions == null || actions.isEmpty()) {
            return 0;
        }
        return actions.stream()
                .mapToInt(ResourceAction::getImpliedMask)
                // 使用按位或(|)合并所有权限
                .reduce(0, (a, b) -> a | b);
    }

    // 快速校验是否拥有某个指定权限
    public static boolean hasAction(int currentPermissionCode, ResourceAction targetAction) {
        return (currentPermissionCode & targetAction.code) != 0;
    }
}
