package com.oriole.wisepen.resource.domain.entity;

import com.oriole.wisepen.resource.domain.base.ResourceInteractionInfoBase;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@Document(collection = "wisepen_resource_interact_info")
public class ResourceInteractionInfoEntity extends ResourceInteractionInfoBase {
    @Id
    private String resourceId;

    @CreatedDate
    private LocalDateTime createTime;
    @LastModifiedDate
    private LocalDateTime updateTime;

    /** 新资源初始化：resourceId 已知，所有计数继承基类默认值 0。 */
    public ResourceInteractionInfoEntity(String resourceId) {
        this.resourceId = resourceId;
    }
}
