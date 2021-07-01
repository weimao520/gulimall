package com.atguigu.gulimall.product.feign;

import com.atguigu.common.to.SkuHasStockTo;
import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * @author WeiMao
 * @create 2021-05-26 15:50
 */
@FeignClient("gulimall-ware")
public interface WareFeignService {

    @PostMapping("/hasStock")
     R getSkusHasStock(@RequestBody List<Long> skuIds);
}
