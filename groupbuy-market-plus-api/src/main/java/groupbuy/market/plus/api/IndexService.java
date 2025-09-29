package groupbuy.market.plus.api;

import groupbuy.market.plus.api.dto.IndexRequestDTO;
import groupbuy.market.plus.api.dto.IndexResponseDTO;
import groupbuy.market.plus.api.response.Response;

public interface IndexService {

    /**
     * 商品详情页面
     * @param indexRequestDTO
     * @return
     */
    Response<IndexResponseDTO> index(IndexRequestDTO indexRequestDTO);

}
