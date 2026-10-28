package groupbuy.market.plus.test.unit.activity;

import groupbuy.market.plus.domain.activity.adapter.repository.ActivityRepository;
import groupbuy.market.plus.domain.activity.model.entity.MarketProductEntity;
import groupbuy.market.plus.domain.activity.model.valobj.ActivityVO;
import groupbuy.market.plus.domain.activity.model.valobj.SCSkuActivityVO;
import groupbuy.market.plus.domain.activity.service.trial.factory.DefaultActivityStrategyFactory;
import groupbuy.market.plus.domain.activity.service.trial.node.TagNode;
import groupbuy.market.plus.infrastructure.adapter.repository.ActivityRepositoryImpl;
import groupbuy.market.plus.infrastructure.dao.ActivityDao;
import groupbuy.market.plus.infrastructure.dao.DiscountDao;
import groupbuy.market.plus.infrastructure.dao.po.Activity;
import groupbuy.market.plus.infrastructure.dao.po.Discount;
import groupbuy.market.plus.types.enums.ResponseCodeEnum;
import groupbuy.market.plus.types.exception.AppException;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ThreadPoolExecutor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 活动折扣配置缺失单元测试 - 折扣不存在时返回 E0002，而不是空指针 / 未知错误
 */
public class DiscountMissingUnitTest {

    @Test
    public void test_discountNotExist_E0002() {
        ActivityRepositoryImpl repository = buildRepository("1,2");
        DiscountDao discountDao = (DiscountDao) ReflectionTestUtils.getField(repository, "discountDao");
        when(discountDao.getDiscountById("1")).thenReturn(Discount.builder().discountName("直减").discountType(0).build());
        when(discountDao.getDiscountById("2")).thenReturn(null);

        try {
            repository.getActivityById(100123L);
            Assert.fail("应抛出 E0002");
        } catch (AppException e) {
            Assert.assertEquals(ResponseCodeEnum.E0002.getCode(), e.getCode());
        }
    }

    @Test
    public void test_blankDiscountExpr_emptyDiscountList() {
        for (String blank : new String[]{null, ""}) {
            ActivityVO activityVO = buildRepository(blank).getActivityById(100123L);
            Assert.assertTrue(activityVO.getGroupBuyDiscountList().isEmpty());
        }
    }

    @Test
    public void test_trial_asyncAppException_unwrap() throws Exception {
        // 异步查询活动时抛出的业务异常应原样抛出，而不是被包装为 ExecutionException
        ActivityRepository activityRepository = mock(ActivityRepository.class);
        when(activityRepository.getSCSkuActivityByGoodsIdWithSC("9890001", "s01", "c01")).thenReturn(SCSkuActivityVO.builder().activityId(100123L).build());
        when(activityRepository.getActivityById(100123L)).thenThrow(new AppException(ResponseCodeEnum.E0002.getCode(), ResponseCodeEnum.E0002.getInfo()));
        ThreadPoolExecutor executor = mock(ThreadPoolExecutor.class);
        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(executor).execute(any(Runnable.class));
        TagNode tagNode = new TagNode();
        ReflectionTestUtils.setField(tagNode, "threadPoolExecutor", executor);
        ReflectionTestUtils.setField(tagNode, "activityRepository", activityRepository);

        Method multiThread = findMultiThread();
        multiThread.setAccessible(true);
        try {
            multiThread.invoke(tagNode, MarketProductEntity.builder().userId("user01").goodsId("9890001").source("s01").channel("c01").build(),
                    new DefaultActivityStrategyFactory.DynamicContext());
            Assert.fail("应抛出 E0002");
        } catch (InvocationTargetException e) {
            Assert.assertTrue("应为 AppException，实际：" + e.getCause(), e.getCause() instanceof AppException);
            Assert.assertEquals(ResponseCodeEnum.E0002.getCode(), ((AppException) e.getCause()).getCode());
        }
    }

    private ActivityRepositoryImpl buildRepository(String discountExpr) {
        ActivityDao activityDao = mock(ActivityDao.class);
        when(activityDao.getActivityById(100123L)).thenReturn(Activity.builder().activityId(100123L).discountExpr(discountExpr).build());
        ActivityRepositoryImpl repository = new ActivityRepositoryImpl();
        ReflectionTestUtils.setField(repository, "activityDao", activityDao);
        ReflectionTestUtils.setField(repository, "discountDao", mock(DiscountDao.class));
        return repository;
    }

    private Method findMultiThread() throws NoSuchMethodException {
        for (Class<?> c = TagNode.class; c != null; c = c.getSuperclass()) {
            try {
                return c.getDeclaredMethod("multiThread", MarketProductEntity.class, DefaultActivityStrategyFactory.DynamicContext.class);
            } catch (NoSuchMethodException ignored) {
                // 继续向父类查找
            }
        }
        throw new NoSuchMethodException("multiThread");
    }

}
