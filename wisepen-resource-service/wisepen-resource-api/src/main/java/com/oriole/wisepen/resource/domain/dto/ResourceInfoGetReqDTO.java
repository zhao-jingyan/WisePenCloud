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
@NoArgsConstructor
@AllArgsConstructor
public class ResourceInfoGetReqDTO {
    @NotBlank(message = ResourceValidationMsg.RESOURCE_ID_NOT_BLANK)
    private String resourceId;
    @NotNull(message = ResourceValidationMsg.USER_ID_NOT_NULL)
    private Long userId;
    @NotNull(message = ResourceValidationMsg.USER_GROUP_ROLES_NOT_NULL)
    private Map<Long, GroupRoleType> groupRoles;

    private Integer targetVersion;
}
