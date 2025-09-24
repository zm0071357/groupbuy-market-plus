package groupbuy.market.plus.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新配置返回结果
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateConfigDTO {

    /**
     * 配置
     */
    private String key;

    /**
     * 值
     */
    private String value;
}
