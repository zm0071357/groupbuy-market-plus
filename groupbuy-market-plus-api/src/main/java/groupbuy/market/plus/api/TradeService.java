package groupbuy.market.plus.api;

import groupbuy.market.plus.api.dto.LockOrderRequestDTO;
import groupbuy.market.plus.api.dto.LockOrderResponseDTO;
import groupbuy.market.plus.api.response.Response;

public interface TradeService {

    /**
     * 锁单
     * @param lockOrderRequestDTO
     * @return
     */
    Response<LockOrderResponseDTO> lockOrder(LockOrderRequestDTO lockOrderRequestDTO);
}
