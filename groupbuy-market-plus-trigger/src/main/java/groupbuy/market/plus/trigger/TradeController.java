package groupbuy.market.plus.trigger;

import com.alibaba.fastjson.JSON;
import groupbuy.market.plus.api.TradeService;
import groupbuy.market.plus.api.dto.LockOrderRequestDTO;
import groupbuy.market.plus.api.dto.LockOrderResponseDTO;
import groupbuy.market.plus.api.response.Response;
import groupbuy.market.plus.domain.activity.model.entity.MarketProductEntity;
import groupbuy.market.plus.domain.activity.model.entity.TrialBalanceEntity;
import groupbuy.market.plus.domain.activity.service.IndexGroupBuyMarketService;
import groupbuy.market.plus.domain.trade.model.entity.GroupBuyTeamEntity;
import groupbuy.market.plus.domain.trade.model.entity.LockOrderEntity;
import groupbuy.market.plus.domain.trade.model.entity.OrderDetailEntity;
import groupbuy.market.plus.domain.trade.model.entity.UserEntity;
import groupbuy.market.plus.domain.trade.model.valobj.TeamProgressVO;
import groupbuy.market.plus.domain.trade.service.lock.LockOrderService;
import groupbuy.market.plus.types.enums.ResponseCodeEnum;
import groupbuy.market.plus.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@Slf4j
@CrossOrigin("*")
@RestController
@RequestMapping("/trade")
public class TradeController implements TradeService {

    @Resource
    private LockOrderService lockOrderService;

    @Resource
    private IndexGroupBuyMarketService indexGroupBuyMarketService;

    @PostMapping("/lock_order")
    @Override
    public Response<LockOrderResponseDTO> lockOrder(@RequestBody LockOrderRequestDTO lockOrderRequestDTO) {
        try {
            // 参数校验
            if (StringUtils.isBlank(lockOrderRequestDTO.getUserId()) || StringUtils.isBlank(lockOrderRequestDTO.getGoodsId()) ||
                    StringUtils.isBlank(lockOrderRequestDTO.getChannel()) || StringUtils.isBlank(lockOrderRequestDTO.getSource()) ||
                    StringUtils.isBlank(lockOrderRequestDTO.getOutTradeNo()) || lockOrderRequestDTO.getActivityId() == null) {
                return Response.<LockOrderResponseDTO>builder()
                        .code(ResponseCodeEnum.ILLEGAL_PARAMETER.getCode())
                        .info(ResponseCodeEnum.ILLEGAL_PARAMETER.getInfo())
                        .build();
            }

            // 是否有未支付的锁单订单
            LockOrderEntity lockOrderEntity = lockOrderService.getNoPayLockOrderByOutTradeNo(lockOrderRequestDTO.getUserId(), lockOrderRequestDTO.getOutTradeNo());
            if (lockOrderEntity != null) {
                log.info("存在交易锁单记录，用户ID：{} 订单信息:{}", lockOrderRequestDTO.getUserId(), JSON.toJSONString(lockOrderEntity));
                return Response.<LockOrderResponseDTO>builder()
                        .code(ResponseCodeEnum.SUCCESS.getCode())
                        .info(ResponseCodeEnum.SUCCESS.getInfo())
                        .data(LockOrderResponseDTO.builder()
                                .orderId(lockOrderEntity.getOrderId())
                                .payPrice(lockOrderEntity.getPayPrice())
                                .isHeader(lockOrderEntity.getIsHeader() ? 1 : 0)
                                .tradeOrderStatus(lockOrderEntity.getOrderStatusEnum().getCode())
                                .build())
                        .build();
            }

            // 拼团目标量是否完成
            if (!StringUtils.isBlank(lockOrderRequestDTO.getTeamId())) {
                log.info("查询当前组队拼团进度，组队ID：{}", lockOrderRequestDTO.getTeamId());
                TeamProgressVO teamProgressVO = lockOrderService.getTeamProgress(lockOrderRequestDTO.getTeamId());
                if (teamProgressVO == null) {
                    return Response.<LockOrderResponseDTO>builder()
                            .code(ResponseCodeEnum.ILLEGAL_PARAMETER.getCode())
                            .info(ResponseCodeEnum.ILLEGAL_PARAMETER.getInfo())
                            .build();
                }
                if (teamProgressVO.getLockCount() >= teamProgressVO.getTargetCount()) {
                    return Response.<LockOrderResponseDTO>builder()
                            .code(ResponseCodeEnum.E0012.getCode())
                            .info(ResponseCodeEnum.E0012.getInfo())
                            .build();
                }
            }

            // 活动试算
            TrialBalanceEntity trialBalanceEntity = indexGroupBuyMarketService.indexMarketTrial(MarketProductEntity.builder()
                            .userId(lockOrderRequestDTO.getUserId())
                            .goodsId(lockOrderRequestDTO.getGoodsId())
                            .activityId(lockOrderRequestDTO.getActivityId())
                            .source(lockOrderRequestDTO.getSource())
                            .channel(lockOrderRequestDTO.getChannel())
                            .build());

            // 锁单
            lockOrderEntity = lockOrderService.lockOrder(
                    UserEntity.builder().userId(lockOrderRequestDTO.getUserId()).build(),
                    GroupBuyTeamEntity.builder()
                            .teamId(lockOrderRequestDTO.getTeamId())
                            .activityId(lockOrderRequestDTO.getActivityId())
                            .startTime(trialBalanceEntity.getActivityVO().getStartTime())
                            .endTime(trialBalanceEntity.getActivityVO().getEndTime())
                            .validTime(trialBalanceEntity.getActivityVO().getValidTime())
                            .targetCount(trialBalanceEntity.getActivityVO().getTarget())
                            .build(),
                    OrderDetailEntity.builder()
                            .outTradeNo(lockOrderRequestDTO.getOutTradeNo())
                            .source(lockOrderRequestDTO.getSource())
                            .channel(lockOrderRequestDTO.getChannel())
                            .goodsId(trialBalanceEntity.getGoodsId())
                            .goodsName(trialBalanceEntity.getGoodsName())
                            .originalPrice(trialBalanceEntity.getOriginalPrice())
                            .deductionPrice(trialBalanceEntity.getDeductionPrice())
                            .payPrice(trialBalanceEntity.getPayPrice())
                            .build()
            );
            log.info("生成交易锁单记录，用户ID：{} 订单信息:{}", lockOrderRequestDTO.getUserId(), JSON.toJSONString(lockOrderEntity));
            return Response.<LockOrderResponseDTO>builder()
                    .code(ResponseCodeEnum.SUCCESS.getCode())
                    .info(ResponseCodeEnum.SUCCESS.getInfo())
                    .data(LockOrderResponseDTO.builder()
                            .orderId(lockOrderEntity.getOrderId())
                            .payPrice(lockOrderEntity.getPayPrice())
                            .isHeader(lockOrderEntity.getIsHeader() ? 1 : 0)
                            .tradeOrderStatus(lockOrderEntity.getOrderStatusEnum().getCode())
                            .build())
                    .build();
        } catch (AppException e) {
            log.error("锁单业务异常，用户ID：{}，锁单请求信息:{}，错误信息：{}", lockOrderRequestDTO.getUserId(), JSON.toJSONString(lockOrderRequestDTO), e.getInfo());
            return Response.<LockOrderResponseDTO>builder()
                    .code(e.getCode())
                    .info(e.getInfo())
                    .build();
        } catch (Exception e) {
            log.error("锁单业务异常，用户ID：{}，锁单请求信息:{}，错误信息：{}", lockOrderRequestDTO.getUserId(), JSON.toJSONString(lockOrderRequestDTO), e);
            return Response.<LockOrderResponseDTO>builder()
                    .code(ResponseCodeEnum.UN_ERROR.getCode())
                    .info(ResponseCodeEnum.UN_ERROR.getInfo())
                    .build();
        }
    }


}
