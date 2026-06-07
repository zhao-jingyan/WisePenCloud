package com.oriole.wisepen.user.api.feign;

import com.oriole.wisepen.common.core.domain.R;
import com.oriole.wisepen.user.api.domain.dto.req.WalletSettleCoinTradeRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(contextId = "remoteWalletService", value = "wisepen-user-service")
public interface RemoteWalletService {

    @PostMapping("/internal/user/wallet/settleTrade")
    R<Void> settleCoinTrade(@RequestBody WalletSettleCoinTradeRequest req);
}
