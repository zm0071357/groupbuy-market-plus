package groupbuy.market.plus.test.unit.mapper;

import groupbuy.market.plus.infrastructure.dao.GroupBuyTeamDao;
import org.apache.ibatis.reflection.ParamNameResolver;
import org.apache.ibatis.session.Configuration;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 组队ID列表参数绑定单元测试 - XML 中 foreach collection="teamIdList" 必须能取到参数
 */
public class TeamIdListParamUnitTest {

    @Test
    public void test_teamIdList_bound() throws Exception {
        Configuration configuration = new Configuration();
        for (String methodName : new String[]{"getRandomTeam"}) {
            Method method = GroupBuyTeamDao.class.getMethod(methodName, List.class);
            Object params = new ParamNameResolver(configuration, method).getNamedParams(new Object[]{Collections.singletonList("TEAM12345678")});

            Assert.assertTrue(methodName + " 参数应为命名参数", params instanceof Map);
            Assert.assertTrue(methodName + " 应能按 teamIdList 取到参数", ((Map<?, ?>) params).containsKey("teamIdList"));
        }
    }

    @Test
    public void test_compiler_keepParameterNames() throws Exception {
        // 编译插件继承 Spring Boot parent 的 parameters=true 后，未加 @Param 的方法也保留真实参数名
        Method method = GroupBuyTeamDao.class.getMethod("getTeamById", String.class);
        Assert.assertTrue("编译时应保留方法参数名（-parameters）", method.getParameters()[0].isNamePresent());
        Assert.assertEquals("teamId", method.getParameters()[0].getName());
    }

}
