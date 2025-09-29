package groupbuy.market.plus.infrastructure.gateway;

import groupbuy.market.plus.types.enums.ResponseCodeEnum;
import groupbuy.market.plus.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 回调网关
 */
@Slf4j
@Service
public class NotifyTaskServiceImpl {

    @Resource
    private OkHttpClient okHttpClient;

    /**
     * 回调
     * @param notifyUrl 回调地址
     * @param requestJson 参数
     * @return
     */
    public String notify(String notifyUrl, String requestJson) {
        try {
            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json"),
                    requestJson);
            Request request = new Request.Builder()
                    .url(notifyUrl)
                    .post(body)
                    .addHeader("content-type", "application/json")
                    .build();
            Response response = okHttpClient.newCall(request).execute();
            return response.body().string();
        } catch (Exception e) {
            log.error("拼团回调 HTTP 接口服务异常 {}", notifyUrl, e);
            throw new AppException(ResponseCodeEnum.HTTP_EXCEPTION.getCode(), ResponseCodeEnum.HTTP_EXCEPTION.getInfo());
        }
    }

}
