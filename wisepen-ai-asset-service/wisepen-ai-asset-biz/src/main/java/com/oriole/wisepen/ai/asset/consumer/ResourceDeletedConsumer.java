package com.oriole.wisepen.ai.asset.consumer;

import com.oriole.wisepen.resource.domain.mq.ResourceDeletedMessage;
import com.oriole.wisepen.resource.enums.ResourceType;
import com.oriole.wisepen.ai.asset.service.ISkillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.oriole.wisepen.resource.constant.MqTopicConstants.TOPIC_RESOURCE_PHYSICAL_DESTROY;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResourceDeletedConsumer {

    private final ISkillService skillService;

    @KafkaListener(topics = TOPIC_RESOURCE_PHYSICAL_DESTROY, groupId = "wisepen-skill-resource-destroy-group")
    public void onResourceDeleted(ResourceDeletedMessage message) {
        Map<ResourceType, List<String>> typedMap = message.getTypedResourceIds();
        List<String> skillIds = typedMap.get(ResourceType.SKILL);
        if (skillIds != null && !skillIds.isEmpty()) {
            log.info("接收到 Skill 资源硬删除事件 skillIds={}", skillIds);
            skillService.deleteSkills(skillIds);
            log.info("已处理 Skill 资源硬删除事件 skillIds={}", skillIds);
        }
    }
}