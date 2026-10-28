package groupbuy.market.plus.test.unit.mapper;

import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.session.Configuration;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

/**
 * 随机组队查询结果映射单元测试 - 查询的每一列都必须有显式映射
 */
public class RandomTeamResultMapUnitTest {

    @Test
    public void test_getRandomTeam_useDataMap() throws Exception {
        Configuration configuration = MapperTestSupport.parse("mybatis/mapper/group_buy_team_mapper.xml");
        String statementId = "groupbuy.market.plus.infrastructure.dao.GroupBuyTeamDao.getRandomTeam";
        ResultMap resultMap = configuration.getMappedStatement(statementId).getResultMaps().get(0);

        Assert.assertEquals("groupbuy.market.plus.infrastructure.dao.GroupBuyTeamDao.dataMap", resultMap.getId());
        Set<String> mappedColumns = new HashSet<>();
        for (ResultMapping resultMapping : resultMap.getResultMappings()) {
            mappedColumns.add(resultMapping.getColumn());
        }
        for (String column : new String[]{"team_id", "target_count", "complete_count", "lock_count", "start_time", "end_time"}) {
            Assert.assertTrue("缺少列映射：" + column, mappedColumns.contains(column));
        }
    }

}
