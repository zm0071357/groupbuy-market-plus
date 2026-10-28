package groupbuy.market.plus.test.unit.mapper;

import groupbuy.market.plus.infrastructure.dao.po.GroupBuyTeam;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.session.Configuration;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * 团长退单补偿回调配置持久化单元测试 - 锁单写入、查询组队读出、结果映射三处一致
 */
public class HeaderRefundNotifyColumnUnitTest {

    private static final String NAMESPACE = "groupbuy.market.plus.infrastructure.dao.GroupBuyTeamDao.";

    @Test
    public void test_headerRefundNotify_persisted() throws Exception {
        Configuration configuration = MapperTestSupport.parse("mybatis/mapper/group_buy_team_mapper.xml");

        GroupBuyTeam team = new GroupBuyTeam();
        team.setHeaderRefundNotifyUrl("http://127.0.0.1/header_refund_notify");
        String insertSql = MapperTestSupport.sql(configuration, NAMESPACE + "insert", team);
        Assert.assertTrue(insertSql, insertSql.contains("header_refund_notify_url, header_refund_notify_mq"));
        // 插入参数中包含团长退单回调地址
        boolean bound = configuration.getMappedStatement(NAMESPACE + "insert").getBoundSql(team).getParameterMappings().stream()
                .anyMatch(parameterMapping -> "headerRefundNotifyUrl".equals(parameterMapping.getProperty()));
        Assert.assertTrue("insert 应绑定 headerRefundNotifyUrl", bound);

        String selectSql = MapperTestSupport.sql(configuration, NAMESPACE + "getTeamById", "TEAM12345678");
        Assert.assertTrue(selectSql, selectSql.contains("refund_notify_mq, header_refund_notify_url, header_refund_notify_mq"));

        Map<String, String> columnToProperty = new HashMap<>();
        for (ResultMapping resultMapping : configuration.getResultMap(NAMESPACE + "dataMap").getResultMappings()) {
            columnToProperty.put(resultMapping.getColumn(), resultMapping.getProperty());
        }
        Assert.assertEquals("headerRefundNotifyUrl", columnToProperty.get("header_refund_notify_url"));
        Assert.assertEquals("headerRefundNotifyMQ", columnToProperty.get("header_refund_notify_mq"));
    }

}
