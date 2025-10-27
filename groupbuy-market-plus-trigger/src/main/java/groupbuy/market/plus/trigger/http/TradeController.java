package groupbuy.market.plus.trigger.http;

import com.alibaba.fastjson.JSON;
import groupbuy.market.plus.api.TradeService;
import groupbuy.market.plus.api.dto.*;
import groupbuy.market.plus.api.response.Response;
import groupbuy.market.plus.domain.activity.model.entity.MarketProductEntity;
import groupbuy.market.plus.domain.activity.model.entity.TrialBalanceEntity;
import groupbuy.market.plus.domain.activity.service.IndexGroupBuyMarketService;
import groupbuy.market.plus.domain.trade.model.entity.*;
import groupbuy.market.plus.domain.trade.model.valobj.NotifyConfigVO;
import groupbuy.market.plus.domain.trade.model.valobj.NotifyTypeEnum;
import groupbuy.market.plus.domain.trade.model.valobj.TeamProgressVO;
import groupbuy.market.plus.domain.trade.service.lock.LockOrderService;
import groupbuy.market.plus.domain.trade.service.refund.RefundOrderService;
import groupbuy.market.plus.domain.trade.service.settle.SettleOrderService;
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
    private SettleOrderService settleOrderService;

    @Resource
    private RefundOrderService refundOrderService;

    @Resource
    private IndexGroupBuyMarketService indexGroupBuyMarketService;

    @PostMapping("/lock_order")
    @Override
    public Response<LockOrderResponseDTO> lockOrder(@RequestBody LockOrderRequestDTO lockOrderRequestDTO) {
        try {
            // 参数校验
            if (StringUtils.isBlank(lockOrderRequestDTO.getUserId()) || StringUtils.isBlank(lockOrderRequestDTO.getGoodsId()) ||
                    StringUtils.isBlank(lockOrderRequestDTO.getChannel()) || StringUtils.isBlank(lockOrderRequestDTO.getSource()) ||
                    StringUtils.isBlank(lockOrderRequestDTO.getOutTradeNo()) || lockOrderRequestDTO.getNotifyConfig() == null ||
                    lockOrderRequestDTO.getActivityId() == null) {
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
                                .originalPrice(lockOrderEntity.getOriginalPrice())
                                .deductionPrice(lockOrderEntity.getDeductionPrice())
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
                            .goodsId(lockOrderRequestDTO.getGoodsId())
                            .startTime(trialBalanceEntity.getActivityVO().getStartTime())
                            .endTime(trialBalanceEntity.getActivityVO().getEndTime())
                            .validTime(trialBalanceEntity.getActivityVO().getValidTime())
                            .targetCount(trialBalanceEntity.getActivityVO().getTarget())
                            .notifyConfigVO(NotifyConfigVO.builder()
                                    .notifyTypeEnum(NotifyTypeEnum.getByNotifyCode(lockOrderRequestDTO.getNotifyConfig().getNotifyType()))
                                    .notifyUrl(lockOrderRequestDTO.getNotifyConfig().getNotifyUrl())
                                    .notifyMQ(lockOrderRequestDTO.getNotifyConfig().getNotifyMQ())
                                    .refundNotifyMQ(lockOrderRequestDTO.getNotifyConfig().getRefundNotifyMQ())
                                    .headerRefundNotifyUrl(lockOrderRequestDTO.getNotifyConfig().getHeaderRefundNotifyUrl())
                                    .headerRefundNotifyMQ(lockOrderRequestDTO.getNotifyConfig().getHeaderRefundNotifyMQ())
                                    .build())
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
                            .originalPrice(lockOrderEntity.getOriginalPrice())
                            .deductionPrice(lockOrderEntity.getDeductionPrice())
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

    @PostMapping("/settle_order")
    @Override
    public Response<SettleOrderResponseDTO> settleOrder(@RequestBody SettleOrderRequestDTO settleOrderRequestDTO) {
        try {
            // 参数校验
            if (StringUtils.isBlank(settleOrderRequestDTO.getUserId()) || StringUtils.isBlank(settleOrderRequestDTO.getOutTradeNo()) || StringUtils.isBlank(settleOrderRequestDTO.getSource()) ||
                StringUtils.isBlank(settleOrderRequestDTO.getChannel()) || settleOrderRequestDTO.getOutTradeNoPayTime() == null) {
                return Response.<SettleOrderResponseDTO>builder()
                        .code(ResponseCodeEnum.ILLEGAL_PARAMETER.getCode())
                        .info(ResponseCodeEnum.ILLEGAL_PARAMETER.getInfo())
                        .build();
            }
            // 结算
            SettleOrderEntity settleOrderEntity = settleOrderService.settleOrder(OrderPaySuccessEntity.builder()
                            .userId(settleOrderRequestDTO.getUserId())
                            .outTradeNo(settleOrderRequestDTO.getOutTradeNo())
                            .outTradeNoPayTime(settleOrderRequestDTO.getOutTradeNoPayTime())
                            .source(settleOrderRequestDTO.getSource())
                            .channel(settleOrderRequestDTO.getChannel())
                            .build());
            return Response.<SettleOrderResponseDTO>builder()
                    .code(ResponseCodeEnum.SUCCESS.getCode())
                    .info(ResponseCodeEnum.SUCCESS.getInfo())
                    .data(SettleOrderResponseDTO.builder()
                            .userId(settleOrderEntity.getUserId())
                            .teamId(settleOrderEntity.getTeamId())
                            .activityId(settleOrderEntity.getActivityId())
                            .outTradeNo(settleOrderEntity.getOutTradeNo())
                            .outTradeNoPayTime(settleOrderEntity.getOutTradeNoPayTime())
                            .isComplete(settleOrderEntity.getIsComplete())
                            .build())
                    .build();
        } catch (AppException e) {
            log.error("结算业务异常：{} 结算请求信息：{} 错误信息：{}", settleOrderRequestDTO.getUserId(), JSON.toJSONString(settleOrderRequestDTO), e.getInfo());
            return Response.<SettleOrderResponseDTO>builder()
                    .code(e.getCode())
                    .info(e.getInfo())
                    .build();
        } catch (Exception e) {
            log.error("结算业务异常：{} 结算请求信息：{}", settleOrderRequestDTO.getUserId(), JSON.toJSONString(settleOrderRequestDTO), e);
            return Response.<SettleOrderResponseDTO>builder()
                    .code(ResponseCodeEnum.UN_ERROR.getCode())
                    .info(ResponseCodeEnum.UN_ERROR.getInfo())
                    .build();
        }
    }

    @PostMapping("/refund_order")
    @Override
    public Response<RefundOrderResponseDTO> refundOrder(@RequestBody RefundOrderRequestDTO refundOrderRequestDTO) {
        try {
            // 参数校验
            if (StringUtils.isBlank(refundOrderRequestDTO.getUserId()) || StringUtils.isBlank(refundOrderRequestDTO.getOutTradeNo()) ||
                    StringUtils.isBlank(refundOrderRequestDTO.getSource()) || StringUtils.isBlank(refundOrderRequestDTO.getChannel())) {
                return Response.<RefundOrderResponseDTO>builder()
                        .code(ResponseCodeEnum.ILLEGAL_PARAMETER.getCode())
                        .info(ResponseCodeEnum.ILLEGAL_PARAMETER.getInfo())
                        .build();
            }
            RefundResEntity refundResEntity = refundOrderService.refundOrder(PreRefundEntity.builder()
                            .userId(refundOrderRequestDTO.getUserId())
                            .outTradeNo(refundOrderRequestDTO.getOutTradeNo())
                            .source(refundOrderRequestDTO.getSource())
                            .channel(refundOrderRequestDTO.getChannel())
                            .build());
            return Response.<RefundOrderResponseDTO>builder()
                    .code(ResponseCodeEnum.SUCCESS.getCode())
                    .data(RefundOrderResponseDTO.builder()
                            .userId(refundResEntity.getUserId())
                            .orderId(refundResEntity.getOrderId())
                            .outTradeNo(refundOrderRequestDTO.getOutTradeNo())
                            .teamId(refundResEntity.getTeamId())
                            .refundCode(refundResEntity.getRefundStatusEnum().getCode())
                            .refundInfo(refundResEntity.getRefundStatusEnum().getInfo())
                            .build())
                    .info(ResponseCodeEnum.SUCCESS.getInfo())
                    .build();
        } catch (AppException e) {
            log.error("退单业务异常：{} 退单请求信息：{} 错误信息：{}", refundOrderRequestDTO.getUserId(), JSON.toJSONString(refundOrderRequestDTO), e.getInfo());
            return Response.<RefundOrderResponseDTO>builder()
                    .code(e.getCode())
                    .info(e.getInfo())
                    .build();
        } catch (Exception e) {
            log.error("退单业务异常：{} 退单请求信息：{}", refundOrderRequestDTO.getUserId(), JSON.toJSONString(refundOrderRequestDTO), e);
            return Response.<RefundOrderResponseDTO>builder()
                    .code(ResponseCodeEnum.UN_ERROR.getCode())
                    .info(ResponseCodeEnum.UN_ERROR.getInfo())
                    .build();
        }
    }

    @PostMapping("/team_progress/{teamId}")
    @Override
    public Response<TeamProgressResponseDTO> getTeamProgress(@PathVariable("teamId") String teamId) {
        try {
            // 参数校验
            if (StringUtils.isBlank(teamId)) {
                return Response.<TeamProgressResponseDTO>builder()
                        .code(ResponseCodeEnum.ILLEGAL_PARAMETER.getCode())
                        .info(ResponseCodeEnum.ILLEGAL_PARAMETER.getInfo())
                        .build();
            }
            // 获取拼团组队进度
            TeamProgressVO teamProgressVO = refundOrderService.getTeamProgress(teamId);
            return Response.<TeamProgressResponseDTO>builder()
                    .code(ResponseCodeEnum.SUCCESS.getCode())
                    .data(TeamProgressResponseDTO.builder()
                            .status(teamProgressVO.getStatus())
                            .targetCount(teamProgressVO.getTargetCount())
                            .completeCount(teamProgressVO.getCompleteCount())
                            .lockCount(teamProgressVO.getLockCount())
                            .build())
                    .info(ResponseCodeEnum.SUCCESS.getInfo())
                    .build();
        } catch (AppException e) {
            log.error("查询拼团组队进度异常，拼团组队ID：{} 错误信息：{}", teamId, e.getInfo());
            return Response.<TeamProgressResponseDTO>builder()
                    .code(e.getCode())
                    .info(e.getInfo())
                    .build();
        } catch (Exception e) {
            log.error("查询拼团组队进度异常，拼团组队ID：{}", teamId, e);
            return Response.<TeamProgressResponseDTO>builder()
                    .code(ResponseCodeEnum.UN_ERROR.getCode())
                    .info(ResponseCodeEnum.UN_ERROR.getInfo())
                    .build();
        }
    }

}
