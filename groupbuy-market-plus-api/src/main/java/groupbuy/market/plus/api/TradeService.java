package groupbuy.market.plus.api;

import groupbuy.market.plus.api.dto.*;
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

    /**
     * 退单
     * @param refundOrderRequestDTO
     * @return
     */
    Response<RefundOrderResponseDTO> refundOrder(RefundOrderRequestDTO refundOrderRequestDTO);

    /**
     * 获取拼团进度
     * @param teamId 拼团组队ID
     * @return
     */
    Response<TeamProgressResponseDTO> getTeamProgress(String teamId);

    /**
     * 生成邀请码
     * @param inviteRequestDTO
     * @return
     */
    Response<InviteResponseDTO> invite(InviteRequestDTO inviteRequestDTO);

}
