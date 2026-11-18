package groupbuy.market.plus.test.unit.activity;

import com.fasterxml.jackson.databind.ObjectMapper;
import groupbuy.market.plus.domain.activity.adapter.repository.ActivityAdminRepository;
import groupbuy.market.plus.domain.activity.model.entity.ManagedActivityEntity;
import groupbuy.market.plus.domain.activity.model.valobj.ActivityDraftVO;
import groupbuy.market.plus.domain.activity.model.valobj.GroupBuyTierVO;
import groupbuy.market.plus.domain.activity.service.admin.ActivityAdminService;
import groupbuy.market.plus.trigger.http.ActivityAdminAuthConfig;
import groupbuy.market.plus.trigger.http.ActivityAdminController;
import groupbuy.market.plus.types.exception.AppException;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 验证后台身份校验、预览与发布状态转换。 */
public class ActivityAdminUnitTest {
    private static final String TOKEN = "test-activity-management-bearer-token-123456";
    private static final String BASE = "/admin/group-buy/activities";
    private static final String DRAFT_JSON = """
            {"name":"首发团","goodsId":"9890001","source":"s01","channel":"c01",
             "startTime":"2026-11-18T00:00:00Z","endTime":"2099-01-01T00:00:00Z",
             "validMinutes":30,"userLimit":1,
             "tiers":[{"tierId":2,"targetCount":5,"memberPrice":69},{"tierId":1,"targetCount":3,"memberPrice":79}]}
            """;
    private final ActivityAdminRepository repository = mock(ActivityAdminRepository.class);
    private final ActivityAdminService service = new ActivityAdminService();

    @Before
    public void init() {
        ReflectionTestUtils.setField(service, "repository", repository);
        when(repository.product(any())).thenReturn(new ActivityAdminRepository.Product(
                1L, "9890001", "示例商品", "s01", "c01", new BigDecimal("100")));
    }

    @Test
    public void requiresBackendCredentialAndCalculatesPreview() throws Exception {
        MockMvc mvc = mvc(TOKEN);
        mvc.perform(get(BASE)).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("E0043"));
        mvc("").perform(get(BASE).header("Authorization", "Bearer " + TOKEN)).andExpect(status().isUnauthorized());
        mvc.perform(post(BASE + "/preview").header("Authorization", "Bearer " + TOKEN)
                        .contentType("application/json").content(DRAFT_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("0000"))
                .andExpect(jsonPath("$.data.tiers[0].tierId").value(1))
                .andExpect(jsonPath("$.data.tiers[0].leaderPrice").value(75.05));
        verify(repository, never()).create(any());
    }

    @Test
    public void publishesAndAllowsSafeRetry() {
        ActivityDraftVO draft = service.preview(draft());
        when(repository.find(1L)).thenReturn(new ManagedActivityEntity(1L, null, ManagedActivityEntity.State.DRAFT, draft));
        when(repository.publish(1L, draft)).thenReturn(100L);
        assertEquals(100L, service.publish(1L));

        when(repository.find(1L)).thenReturn(new ManagedActivityEntity(1L, 100L, ManagedActivityEntity.State.PUBLISHED, draft));
        assertEquals(100L, service.publish(1L));
        verify(repository, times(1)).publish(1L, draft);

        when(repository.offline(1L)).thenReturn(true);
        service.offline(1L);
        when(repository.find(1L)).thenReturn(new ManagedActivityEntity(1L, 100L, ManagedActivityEntity.State.OFFLINE, draft));
        assertEquals("E0041", assertThrows(AppException.class, () -> service.publish(1L)).getCode());
    }

    @Test
    public void previewsIndependentSkipPriceAndRejectsInvalidConfiguration() throws Exception {
        MockMvc mvc = mvc(TOKEN);
        String enabled = DRAFT_JSON.replace("\"validMinutes\":30", "\"skipGroupEnabled\":true,\"skipGroupPrice\":75,\"validMinutes\":30");
        mvc.perform(post(BASE + "/preview").header("Authorization", "Bearer " + TOKEN)
                        .contentType("application/json").content(enabled))
                .andExpect(jsonPath("$.data.skipGroupPrice").value(75));
        mvc.perform(post(BASE + "/preview").header("Authorization", "Bearer " + TOKEN)
                        .contentType("application/json").content(enabled.replace("75", "101")))
                .andExpect(jsonPath("$.code").value("E0044"));
    }

    private MockMvc mvc(String token) {
        ActivityAdminController controller = new ActivityAdminController();
        ReflectionTestUtils.setField(controller, "service", service);
        class Registry extends InterceptorRegistry {
            List<Object> interceptors() { return getInterceptors(); }
        }
        Registry registry = new Registry();
        new ActivityAdminAuthConfig(token, new ObjectMapper()).addInterceptors(registry);
        return MockMvcBuilders.standaloneSetup(controller)
                .addInterceptors(registry.interceptors().stream().map(HandlerInterceptor.class::cast)
                        .toArray(HandlerInterceptor[]::new)).build();
    }

    private ActivityDraftVO draft() {
        return new ActivityDraftVO("首发团", "9890001", "s01", "c01", Instant.now(),
                Instant.now().plusSeconds(3600), 30, 1, List.of(new GroupBuyTierVO(1L, 3, new BigDecimal("79"))), null, null);
    }
}
