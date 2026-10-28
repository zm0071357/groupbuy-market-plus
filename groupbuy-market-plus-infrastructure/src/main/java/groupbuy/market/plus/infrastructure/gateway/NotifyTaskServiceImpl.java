package groupbuy.market.plus.infrastructure.gateway;

import groupbuy.market.plus.domain.trade.model.valobj.NofifyStatusEnum;
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
     * @return 回调结果：HTTP 状态码为 2xx 且响应体为 success（忽略大小写与首尾空白）时返回 SUCCESS，否则返回 FAIL
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
            // try-with-resources 确保响应关闭、连接归还连接池
            try (Response response = okHttpClient.newCall(request).execute()) {
                ResponseBody responseBody = response.body();
                String result = responseBody == null ? null : responseBody.string();
                if (!response.isSuccessful()) {
                    log.info("拼团回调 HTTP 接口返回非成功状态码，回调地址：{}，状态码：{}，响应：{}", notifyUrl, response.code(), result);
                    return NofifyStatusEnum.FAIL.getStatus();
                }
                if (result == null || !NofifyStatusEnum.SUCCESS.getStatus().equalsIgnoreCase(result.trim())) {
                    log.info("拼团回调 HTTP 接口响应非成功标识，回调地址：{}，响应：{}", notifyUrl, result);
                    return NofifyStatusEnum.FAIL.getStatus();
                }
                return NofifyStatusEnum.SUCCESS.getStatus();
            }
        } catch (Exception e) {
            log.error("拼团回调 HTTP 接口服务异常 {}", notifyUrl, e);
            throw new AppException(ResponseCodeEnum.HTTP_EXCEPTION.getCode(), ResponseCodeEnum.HTTP_EXCEPTION.getInfo());
        }
    }

}
