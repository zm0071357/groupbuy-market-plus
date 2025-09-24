package groupbuy.market.plus.api;

import groupbuy.market.plus.api.dto.UpdateConfigDTO;
import groupbuy.market.plus.api.response.Response;

public interface DCCService {

    /**
     * 更新配置
     * @param key
     * @param value
     * @return
     */
    Response<UpdateConfigDTO> updateConfig(String key, String value);

}
