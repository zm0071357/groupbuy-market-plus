package groupbuy.market.plus.test.unit.activity;

import groupbuy.market.plus.domain.activity.model.valobj.GroupBuyTierVO;
import groupbuy.market.plus.infrastructure.adapter.repository.ActivityRepositoryImpl;
import groupbuy.market.plus.infrastructure.dao.SCSkuActivityDao;
import groupbuy.market.plus.infrastructure.dao.po.SCSkuActivity;
import groupbuy.market.plus.types.exception.AppException;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/** 验证活动映射中的档位 JSON 能进入领域，并在使用前校验配置。 */
public class TierConfigurationUnitTest {

    @Test
    public void readsTierConfigurationAndKeepsLegacyMappingsEmpty() {
        SCSkuActivityDao dao = mock(SCSkuActivityDao.class);
        ActivityRepositoryImpl repository = new ActivityRepositoryImpl();
        ReflectionTestUtils.setField(repository, "scSkuActivityDao", dao);
        SCSkuActivity mapping = SCSkuActivity.builder().activityId(100123L).goodsId("9890001").build();
        when(dao.getSCSkuActivityByIdWithSC(any())).thenReturn(mapping);

        assertTrue(repository.getSCSkuActivityByGoodsIdWithSC("9890001", "s01", "c01").getTiers().isEmpty());
        mapping.setTiersJson("""
                [{"tierId":2,"targetCount":5,"memberPrice":69},
                 {"tierId":1,"targetCount":3,"memberPrice":79}]
                """);
        List<GroupBuyTierVO> tiers = repository.getSCSkuActivityByGoodsIdWithSC("9890001", "s01", "c01").getTiers();

        assertEquals(List.of(
                new GroupBuyTierVO(1L, 3, new BigDecimal("79.00")),
                new GroupBuyTierVO(2L, 5, new BigDecimal("69.00"))),
                GroupBuyTierVO.validate(tiers, new BigDecimal("100")));
    }

    @Test
    public void rejectsInvalidTierPricesTargetsAndIds() {
        BigDecimal originalPrice = new BigDecimal("100");
        GroupBuyTierVO first = new GroupBuyTierVO(1L, 3, new BigDecimal("79"));
        for (GroupBuyTierVO invalid : List.of(
                new GroupBuyTierVO(1L, 5, new BigDecimal("69")),
                new GroupBuyTierVO(2L, 3, new BigDecimal("69")),
                new GroupBuyTierVO(2L, 5, new BigDecimal("101")),
                new GroupBuyTierVO(2L, 5, new BigDecimal("69.001")),
                new GroupBuyTierVO(2L, 5, new BigDecimal("80")))) {
            AppException error = assertThrows(AppException.class,
                    () -> GroupBuyTierVO.validate(List.of(first, invalid), originalPrice));
            assertEquals("E0036", error.getCode());
        }
    }
}
