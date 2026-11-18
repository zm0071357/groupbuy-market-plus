package groupbuy.market.plus.trigger.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import groupbuy.market.plus.api.response.Response;
import groupbuy.market.plus.types.enums.ResponseCodeEnum;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** 管理面只接受独立后台服务端持有的凭据。 */
@Configuration
public class ActivityAdminAuthConfig implements WebMvcConfigurer {

    private final byte[] expected;
    private final ObjectMapper objectMapper;

    /** 缺少有效配置时关闭管理接口访问。 */
    public ActivityAdminAuthConfig(@Value("${GROUPBUY_ADMIN_TOKEN:}") String token, ObjectMapper objectMapper) {
        this.expected = token.length() >= 32 ? ("Bearer " + token).getBytes(StandardCharsets.UTF_8) : new byte[0];
        this.objectMapper = objectMapper;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
                String authorization = request.getHeader("Authorization");
                if (expected.length != 0 && authorization != null
                        && MessageDigest.isEqual(expected, authorization.getBytes(StandardCharsets.UTF_8))) {
                    return true;
                }
                ResponseCodeEnum code = ResponseCodeEnum.E0043;
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                objectMapper.writeValue(response.getWriter(), Response.builder().code(code.getCode()).info(code.getInfo()).build());
                return false;
            }
        }).addPathPatterns("/admin/group-buy", "/admin/group-buy/**");
    }
}
