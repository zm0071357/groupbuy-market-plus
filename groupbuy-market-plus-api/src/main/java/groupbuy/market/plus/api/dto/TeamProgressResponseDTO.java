package groupbuy.market.plus.api.dto;

import lombok.*;

/**
 * 拼团进度响应体
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TeamProgressResponseDTO {

    /**
     * 拼团状态
     */
    private Integer status;

    /**
     * 目标量
     */
    private Integer targetCount;

    /**
     * 完成量
     */
    private Integer completeCount;

    /**
     * 锁单量
     */
    private Integer lockCount;

}
