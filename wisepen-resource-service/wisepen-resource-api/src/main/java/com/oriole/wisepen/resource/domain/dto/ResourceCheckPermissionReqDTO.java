package com.oriole.wisepen.resource.domain.dto;

import com.oriole.wisepen.common.core.domain.enums.GroupRoleType;
import com.oriole.wisepen.resource.constant.ResourceValidationMsg;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Map;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class ResourceCheckPermissionReqDTO {
    @NotBlank(message = ResourceValidationMsg.RESOURCE_ID_NOT_BLANK)
    private String resourceId;
    @NotNull(message = ResourceValidationMsg.USER_ID_NOT_NULL)
    private Long userId;
    @NotNull(message = ResourceValidationMsg.USER_GROUP_ROLES_NOT_NULL)
    private Map<Long, GroupRoleType> groupRoles;
    private Integer version;

    public ResourceCheckPermissionReqDTO(String resourceId, Long userId, Map<Long, GroupRoleType> groupRoles) {
        this.resourceId = resourceId;
        this.userId = userId;
        this.groupRoles = groupRoles;
    }
}
