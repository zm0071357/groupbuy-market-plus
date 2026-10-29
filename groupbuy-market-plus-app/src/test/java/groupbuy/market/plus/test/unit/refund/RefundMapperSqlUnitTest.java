package groupbuy.market.plus.test.unit.refund;

import groupbuy.market.plus.infrastructure.dao.po.GroupBuyTeam;
import groupbuy.market.plus.infrastructure.dao.po.GroupBuyTeamOrder;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;

/**
 * 退单相关 SQL 单元测试 - 不连接数据库，解析 MyBatis 映射文件后检查生成的 SQL
 */
public class RefundMapperSqlUnitTest {

    private Configuration configuration;

    @Before
    public void setUp() throws Exception {
        configuration = new Configuration();
        parse("mybatis/mapper/group_buy_team_mapper.xml");
        parse("mybatis/mapper/group_buy_team_order_mapper.xml");
    }

    @Test
    public void test_completePaidRefund_teamSql() {
        GroupBuyTeam param = new GroupBuyTeam();
        param.setTeamId("TEAM12345678");
        param.setLockCount(-1);
        param.setCompleteCount(-1);
        String sql = sql("groupbuy.market.plus.infrastructure.dao.GroupBuyTeamDao.updateTeamByTeamCompletePaidRefund", param);

        // 仅完成或完成 - 存在退单的组队可更新
        Assert.assertTrue(sql, sql.contains("status in (1, 3)"));
        Assert.assertFalse(sql, sql.contains("status = 0"));
        // 状态按扣减前完成量计算，且在 complete_count 赋值之前
        int statusIndex = sql.indexOf("set status = case when complete_count + ? <= 0 then 2 else 3 end");
        int completeCountIndex = sql.indexOf("complete_count = complete_count + ?");
        Assert.assertTrue(sql, statusIndex >= 0);
        Assert.assertTrue(sql, statusIndex < completeCountIndex);
    }

    @Test
    public void test_refundOrderSql_expectOriginalStatus() {
        GroupBuyTeamOrder param = new GroupBuyTeamOrder();
        param.setUserId("user01");
        param.setOrderId("ORDER000000000001");
        param.setStatus(0);
        String sql = sql("groupbuy.market.plus.infrastructure.dao.GroupBuyTeamOrderDao.updateOrderStatusRefund", param);

        // 按退单策略选定时的原状态更新，不再允许已创建与已支付互相覆盖
        Assert.assertTrue(sql, sql.contains("where user_id = ? and order_id = ? and status = ?"));
        Assert.assertFalse(sql, sql.contains("status in (0, 1)"));
    }

    private void parse(String resource) throws Exception {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resource)) {
            Assert.assertNotNull("找不到映射文件：" + resource, inputStream);
            new XMLMapperBuilder(inputStream, configuration, resource, configuration.getSqlFragments()).parse();
        }
    }

    private String sql(String statementId, Object param) {
        return configuration.getMappedStatement(statementId).getBoundSql(param).getSql().replaceAll("\\s+", " ");
    }

}
