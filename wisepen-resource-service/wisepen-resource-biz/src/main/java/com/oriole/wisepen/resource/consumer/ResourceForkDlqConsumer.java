package com.oriole.wisepen.resource.consumer;

import com.oriole.wisepen.resource.domain.mq.ResourceForkMessage;
import com.oriole.wisepen.resource.service.IMarketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static com.oriole.wisepen.resource.constant.MqTopicConstants.TOPIC_RESOURCE_FORK;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResourceForkDlqConsumer {

    private final IMarketService marketService;

    @KafkaListener(topics = TOPIC_RESOURCE_FORK + ".DLQ", groupId = "wisepen-resource-fork-dlq-group")
    public void onResourceForkDlq(ResourceForkMessage msg) {
        log.warn("resourceFork dlq received. topic={} forkTaskId={} orderId={} sourceResourceId={} resourceType={}",
                TOPIC_RESOURCE_FORK + ".DLQ", msg.getForkTaskId(), msg.getOrderId(), msg.getSourceResourceId(), msg.getResourceType());
        try {
            marketService.compensateFork(msg.getOrderId(), msg.getForkTaskId());
        } catch (Exception ignored) {
            log.warn("market fork compensate failed. orderId={} forkTaskId={} ",
                    msg.getOrderId(), msg.getForkTaskId());
        }
    }
}
