package groupbuy.market.plus.test.unit.trade;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.session.Configuration;
import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * 回调任务 Mapper 映射单元测试 - 不连接数据库，直接解析 MyBatis 映射文件
 */
public class NotifyTaskMapperUnitTest {

    private static final String MAPPER = "mybatis/mapper/notify_task_mapper.xml";

    @Test
    public void test_resultMap_columnToProperty() throws Exception {
        Configuration configuration = new Configuration();
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(MAPPER)) {
            Assert.assertNotNull("找不到映射文件：" + MAPPER, inputStream);
            new XMLMapperBuilder(inputStream, configuration, MAPPER, configuration.getSqlFragments()).parse();
        }
        ResultMap resultMap = configuration.getResultMap("groupbuy.market.plus.infrastructure.dao.NotifyTaskDao.dataMap");

        Map<String, String> columnToProperty = new HashMap<>();
        for (ResultMapping resultMapping : resultMap.getResultMappings()) {
            columnToProperty.put(resultMapping.getColumn(), resultMapping.getProperty());
        }

        // MQ 回调补偿依赖 notifyMQ，不能被映射到 notifyUrl
        Assert.assertEquals("notifyMQ", columnToProperty.get("notify_mq"));
        Assert.assertEquals("notifyUrl", columnToProperty.get("notify_url"));
        Assert.assertEquals("taskId", columnToProperty.get("task_id"));
    }

}
