package groupbuy.market.plus.trigger.http;

import com.alibaba.fastjson.JSON;
import groupbuy.market.plus.api.IndexService;
import groupbuy.market.plus.api.dto.IndexRequestDTO;
import groupbuy.market.plus.api.dto.IndexResponseDTO;
import groupbuy.market.plus.api.response.Response;
import groupbuy.market.plus.domain.activity.model.entity.IndexTeamEntity;
import groupbuy.market.plus.domain.activity.model.entity.MarketProductEntity;
import groupbuy.market.plus.domain.activity.model.entity.TrialBalanceEntity;
import groupbuy.market.plus.domain.activity.model.valobj.StatisticsVO;
import groupbuy.market.plus.domain.activity.service.IndexGroupBuyMarketService;
import groupbuy.market.plus.types.enums.ResponseCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@CrossOrigin("*")
@RestController
@RequestMapping("/index")
public class IndexController implements IndexService {

    @Resource
    private IndexGroupBuyMarketService indexGroupBuyMarketService;

    @PostMapping("/calc")
    @Override
    public Response<IndexResponseDTO> index(@RequestBody IndexRequestDTO indexRequestDTO) {
        try {
            // 参数校验
            if (StringUtils.isBlank(indexRequestDTO.getUserId()) || StringUtils.isBlank(indexRequestDTO.getGoodsId()) ||
                    StringUtils.isBlank(indexRequestDTO.getSource()) || StringUtils.isBlank(indexRequestDTO.getChannel())) {
                return Response.<IndexResponseDTO>builder()
                        .code(ResponseCodeEnum.ILLEGAL_PARAMETER.getCode())
                        .info(ResponseCodeEnum.ILLEGAL_PARAMETER.getInfo())
                        .build();
            }
            log.info("获取商品详情页面数据开始，商品ID：{}", indexRequestDTO.getGoodsId());

            // 试算获取优惠价格
            TrialBalanceEntity trialBalanceEntity = indexGroupBuyMarketService.indexMarketTrial(MarketProductEntity.builder()
                            .userId(indexRequestDTO.getUserId())
                            .goodsId(indexRequestDTO.getGoodsId())
                            .source(indexRequestDTO.getSource())
                            .channel(indexRequestDTO.getChannel())
                            .build());

            Long activityId = trialBalanceEntity.getActivityVO().getActivityId();
            // 查询拼团组队信息
            List<IndexTeamEntity> teamEntityList = indexGroupBuyMarketService.getTeamList(activityId, indexRequestDTO.getUserId(), indexRequestDTO.getGoodsId(), indexRequestDTO.getSource(),
                    indexRequestDTO.getChannel(), 1, 2);
            // 查询统计信息
            StatisticsVO statisticsVO = indexGroupBuyMarketService.getStatistics(activityId, indexRequestDTO.getGoodsId());

            // 组装响应体 - 商品信息
            IndexResponseDTO.Goods goods = IndexResponseDTO.Goods.builder()
                    .goodsId(trialBalanceEntity.getGoodsId())
                    .originalPrice(trialBalanceEntity.getOriginalPrice())
                    .deductionPrice(trialBalanceEntity.getDeductionPrice())
                    .payPrice(trialBalanceEntity.getPayPrice())
                    .build();
            // 组装响应体 - 拼团组队信息
            List<IndexResponseDTO.Team> teamList = new ArrayList<>();
            if (teamEntityList != null && !teamEntityList.isEmpty()) {
                for (IndexTeamEntity indexTeamEntity : teamEntityList) {
                    teamList.add(IndexResponseDTO.Team.builder()
                                    .teamId(indexTeamEntity.getTeamId())
                                    .targetCount(indexTeamEntity.getTargetCount())
                                    .completeCount(indexTeamEntity.getCompleteCount())
                                    .lockCount(indexTeamEntity.getLockCount())
                                    .startTime(indexTeamEntity.getStartTime())
                                    .endTime(indexTeamEntity.getEndTime())
                                    .validTimeCountdown(IndexResponseDTO.Team.getValidTimeCountdown(indexTeamEntity.getStartTime(), indexTeamEntity.getEndTime()))
                                    .build());
                }
            }
            // 组装响应体 - 统计信息
            IndexResponseDTO.Statistics statistics = IndexResponseDTO.Statistics.builder()
                    .allTeamCount(statisticsVO.getAllTeamCount())
                    .allTeamCompleteCount(statisticsVO.getAllTeamCompleteCount())
                    .allTeamUserCount(statisticsVO.getAllTeamUserCount())
                    .build();

            IndexResponseDTO indexResponseDTO = IndexResponseDTO.builder()
                    .goods(goods)
                    .teamList(teamList)
                    .statistics(statistics)
                    .build();
            log.info("获取商品详情页面数据完成：{}", JSON.toJSONString(indexResponseDTO));
            return Response.<IndexResponseDTO>builder()
                    .code(ResponseCodeEnum.SUCCESS.getCode())
                    .info(ResponseCodeEnum.SUCCESS.getInfo())
                    .data(indexResponseDTO)
                    .build();

        } catch (Exception e) {
            log.info("获取商品详情页面数据失败，商品ID：{}，错误信息：{}", indexRequestDTO.getGoodsId(), e);
            return Response.<IndexResponseDTO>builder()
                    .code(ResponseCodeEnum.UN_ERROR.getCode())
                    .info(ResponseCodeEnum.UN_ERROR.getInfo())
                    .build();
        }
    }

}
