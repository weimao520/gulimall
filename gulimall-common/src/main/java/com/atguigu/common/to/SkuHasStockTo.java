package com.atguigu.common.to;

import lombok.Data;

/**
 * @author WeiMao
 * @create 2021-05-26 15:30
 */
@Data
public class SkuHasStockTo {

    private Long skuId;

    private Boolean hasStock;
}
