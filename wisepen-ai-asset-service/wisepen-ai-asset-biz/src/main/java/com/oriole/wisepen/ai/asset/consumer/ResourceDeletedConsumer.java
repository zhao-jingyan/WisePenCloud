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

import static com.oriole.wisepen.common.core.util.LogIdUtils.summarizeIds;
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
        int count = skillIds == null ? 0 : skillIds.size();
        log.info("skill resource delete event received. topic={} count={} skillIds={}",
                TOPIC_RESOURCE_PHYSICAL_DESTROY, count, summarizeIds(skillIds));
        if (count > 0) {
            try {
                skillService.deleteSkills(skillIds);
                log.debug("skill resource delete event consumed. topic={} count={} skillIds={}",
                        TOPIC_RESOURCE_PHYSICAL_DESTROY, count, summarizeIds(skillIds));
            } catch (Exception e) {
                log.error("skill resource delete event consumption failed. topic={} count={} skillIds={}",
                        TOPIC_RESOURCE_PHYSICAL_DESTROY, count, summarizeIds(skillIds), e);
                throw e;
            }
        } else {
            log.debug("skill resource delete event skipped because no skill resources. topic={}",
                    TOPIC_RESOURCE_PHYSICAL_DESTROY);
        }
    }
}
