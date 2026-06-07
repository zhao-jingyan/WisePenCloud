package com.oriole.wisepen.user.consumer;

import com.oriole.wisepen.user.api.domain.mq.TokenConsumptionMessage;
import com.oriole.wisepen.user.service.IWalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static com.oriole.wisepen.user.api.constant.MqTopicConstants.TOPIC_TOKEN_CONSUMPTION;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenConsumptionConsumer {

	private final IWalletService walletService;

	@KafkaListener(topics = TOPIC_TOKEN_CONSUMPTION, groupId = "wisepen-user-token-consumption-group",
			properties = {
					"spring.json.use.type.headers:false",
					"spring.json.value.default.type:com.oriole.wisepen.user.api.domain.mq.TokenConsumptionMessage"
			}
	)
	public void onTokenConsumption(TokenConsumptionMessage message) {
		log.info("token consumption event received. topic={} traceId={}",
				TOPIC_TOKEN_CONSUMPTION, message.getTraceId());
		try {
			walletService.consumptionToken(message);
			log.debug("token consumption event consumed. topic={} traceId={}",
					TOPIC_TOKEN_CONSUMPTION, message.getTraceId());
		} catch (Exception e) {
			log.error("token consumption event consumption failed. topic={} traceId={}",
					TOPIC_TOKEN_CONSUMPTION, message.getTraceId(), e);
			throw e;
		}
	}
}
