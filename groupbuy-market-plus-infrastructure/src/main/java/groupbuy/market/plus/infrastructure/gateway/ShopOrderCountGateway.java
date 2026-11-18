package groupbuy.market.plus.infrastructure.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import groupbuy.market.plus.domain.trade.adapter.port.ShopOrderCountPort;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.OptionalLong;
import java.util.concurrent.TimeUnit;

/** 调用部署侧配置的商城查询接口，无法取得可信计数时返回空。 */
@Slf4j
@Component
public class ShopOrderCountGateway implements ShopOrderCountPort {

    private static final long MAX_RESPONSE_BYTES = 4096;
    private final OkHttpClient client;
    private final ObjectMapper objectMapper;
    private final HttpUrl endpoint;
    private final String token;

    /** 沿用现有连接池，资格查询总超时为 2 秒。 */
    public ShopOrderCountGateway(OkHttpClient client, ObjectMapper objectMapper,
                                 @Value("${groupbuy.customer-order-count.url:}") String url,
                                 @Value("${groupbuy.customer-order-count.token:}") String token) {
        this.client = client.newBuilder().callTimeout(2, TimeUnit.SECONDS)
                .followRedirects(false).followSslRedirects(false).retryOnConnectionFailure(false).build();
        this.objectMapper = objectMapper;
        this.endpoint = HttpUrl.parse(url);
        this.token = token;
    }

    @Override
    public OptionalLong count(String shopId, String userId, String excludedOutTradeNo) {
        if (endpoint == null) {
            return OptionalLong.empty();
        }
        HttpUrl.Builder url = endpoint.newBuilder().addQueryParameter("shopId", shopId).addQueryParameter("userId", userId);
        if (excludedOutTradeNo != null) {
            url.addQueryParameter("excludeOutTradeNo", excludedOutTradeNo);
        }
        Request.Builder request = new Request.Builder().url(url.build()).header("Accept", "application/json");
        if (!token.isBlank()) {
            request.header("Authorization", "Bearer " + token);
        }
        try (Response response = client.newCall(request.build()).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                log.warn("商城下单次数查询失败，状态码：{}", response.code());
                return OptionalLong.empty();
            }
            byte[] body = response.peekBody(MAX_RESPONSE_BYTES + 1).bytes();
            if (body.length > MAX_RESPONSE_BYTES) {
                return OptionalLong.empty();
            }
            JsonNode payload = objectMapper.readTree(body);
            JsonNode value = payload == null ? null : payload.get("orderCount");
            if (value != null && value.isIntegralNumber() && value.canConvertToLong() && value.longValue() >= 0) {
                return OptionalLong.of(value.longValue());
            }
            return OptionalLong.empty();
        } catch (IOException e) {
            log.warn("商城下单次数查询不可用，异常类型：{}", e.getClass().getSimpleName());
            return OptionalLong.empty();
        }
    }
}
