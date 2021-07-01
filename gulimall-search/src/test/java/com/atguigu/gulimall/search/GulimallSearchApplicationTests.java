package com.atguigu.gulimall.search;

import com.atguigu.gulimall.search.config.GulimallElasticSeachConfig;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GulimallSearchApplicationTests {


    @Autowired
    RestHighLevelClient client;

    @Test
  public   void contextLoads() throws IOException {

        IndexRequest indexRequest = new IndexRequest("user_test");
        indexRequest.id("1");

        indexRequest.source("username", "zhangsan", "age", 12, "address", "sz");


        this.client.index(indexRequest, GulimallElasticSeachConfig.COMMON_OPTIONS);

    }

}
