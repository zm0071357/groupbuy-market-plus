package groupbuy.market.plus.test.unit.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.Assert;

import java.io.InputStream;

/**
 * Mapper 单元测试支撑 - 不连接数据库，解析 MyBatis 映射文件后检查 SQL、结果映射与参数绑定
 */
public final class MapperTestSupport {

    private MapperTestSupport() {
    }

    public static Configuration parse(String... resources) throws Exception {
        Configuration configuration = new Configuration();
        for (String resource : resources) {
            try (InputStream inputStream = MapperTestSupport.class.getClassLoader().getResourceAsStream(resource)) {
                Assert.assertNotNull("找不到映射文件：" + resource, inputStream);
                new XMLMapperBuilder(inputStream, configuration, resource, configuration.getSqlFragments()).parse();
            }
        }
        return configuration;
    }

    public static String sql(Configuration configuration, String statementId, Object param) {
        return configuration.getMappedStatement(statementId).getBoundSql(param).getSql().replaceAll("\\s+", " ");
    }

}
