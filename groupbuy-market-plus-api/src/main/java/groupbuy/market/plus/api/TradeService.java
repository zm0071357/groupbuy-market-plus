package groupbuy.market.plus.api;

import groupbuy.market.plus.api.dto.LockOrderRequestDTO;
import groupbuy.market.plus.api.dto.LockOrderResponseDTO;
import groupbuy.market.plus.api.dto.SettleOrderRequestDTO;
import groupbuy.market.plus.api.dto.SettleOrderResponseDTO;
import groupbuy.market.plus.api.response.Response;

public interface TradeService {

    /**
     * 锁单
     * @param lockOrderRequestDTO
     * @return
     */
    Response<LockOrderResponseDTO> lockOrder(LockOrderRequestDTO lockOrderRequestDTO);

    /**
     * 结算
     * @param settleOrderRequestDTO
     * @return
     */
    Response<SettleOrderResponseDTO> settleOrder(SettleOrderRequestDTO settleOrderRequestDTO);

}
