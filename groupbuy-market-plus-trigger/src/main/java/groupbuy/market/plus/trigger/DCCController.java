package groupbuy.market.plus.trigger;

import groupbuy.market.plus.api.DCCService;
import groupbuy.market.plus.api.dto.UpdateConfigDTO;
import groupbuy.market.plus.api.response.Response;
import groupbuy.market.plus.types.common.Constants;
import groupbuy.market.plus.types.enums.ResponseCodeEnum;
import org.redisson.api.RTopic;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@CrossOrigin("*")
@RestController
@RequestMapping("/dcc/config")
public class DCCController implements DCCService {

    @Resource
    private RTopic dccTopicPublisher;

    @PostMapping("/update")
    @Override
    public Response<UpdateConfigDTO> updateConfig(@RequestParam(value = "key") String key,
                                                  @RequestParam(value = "value") String value) {
        try {
            // 推送主题
            dccTopicPublisher.publish(key + Constants.SPLIT + value);
            return Response.<UpdateConfigDTO>builder()
                    .code(ResponseCodeEnum.SUCCESS.getCode())
                    .data(UpdateConfigDTO.builder()
                            .key(key)
                            .value(value)
                            .build())
                    .info(ResponseCodeEnum.SUCCESS.getInfo())
                    .build();
        } catch (Exception e) {
            return Response.<UpdateConfigDTO>builder()
                    .code(ResponseCodeEnum.UN_ERROR.getCode())
                    .info(ResponseCodeEnum.UN_ERROR.getInfo())
                    .build();
        }
    }

}
