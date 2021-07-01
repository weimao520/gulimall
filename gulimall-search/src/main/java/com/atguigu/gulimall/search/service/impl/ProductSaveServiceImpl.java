package com.atguigu.gulimall.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.common.to.es.SkuEsModel;
import com.atguigu.gulimall.search.config.GulimallElasticSeachConfig;
import com.atguigu.gulimall.search.constant.EsConstant;
import com.atguigu.gulimall.search.service.ProductSaveService;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContent;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author WeiMao
 * @create 2021-05-26 16:49
 */
@Slf4j
@Service
public class ProductSaveServiceImpl implements ProductSaveService {

    @Autowired
    RestHighLevelClient restHighLevelClient;

    @Override
    public boolean productStatusUp(List<SkuEsModel> skuEsModels) throws IOException {
        // 保存到es
        //  给es 建立索引，product ，建立映射关系
        // 批量操作
        BulkRequest bulkRequest = new BulkRequest();

            for (SkuEsModel skuEsModel : skuEsModels) {
                IndexRequest indexRequest = new IndexRequest(EsConstant.PRODUCT_INDEX);

                indexRequest.id(skuEsModel.getSpuId().toString());
                String s = JSONObject.toJSONString(skuEsModel);
                indexRequest.source(s, XContentType.JSON);
                bulkRequest.add(indexRequest);
            }
            // bulk 每条语句都是独立运行的
            BulkResponse bulk = this.restHighLevelClient.bulk(bulkRequest, GulimallElasticSeachConfig.COMMON_OPTIONS);
            // todo  处理错误
            boolean b = bulk.hasFailures();
            if (b) {
                List<Object> collect = Arrays.stream(bulk.getItems()).map(BulkItemResponse::getId).collect(Collectors.toList());

                log.error("商品上架错误{}", collect);
            }
            return b;
    }
}
