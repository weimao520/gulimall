package com.atguigu.gulimall.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.common.to.es.SkuEsModel;
import com.atguigu.gulimall.search.config.GulimallElasticSeachConfig;
import com.atguigu.gulimall.search.constant.EsConstant;
import com.atguigu.gulimall.search.service.MallSearchService;
import com.atguigu.gulimall.search.vo.SearchParam;
import com.atguigu.gulimall.search.vo.SearchResult;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author WeiMao
 * @create 2021-06-24 17:07
 */
@Service
public class MallSearchServiceImpl implements MallSearchService {


    @Autowired
    private RestHighLevelClient client;

    @Override
    public SearchResult search(SearchParam searchParam) {

        SearchResult searchResult = null;

        // 准备检索请求
        SearchRequest searchRequest = buildSearchRequest(searchParam);
        try {
            // 执行检索请求
            SearchResponse search = this.client.search(searchRequest, GulimallElasticSeachConfig.COMMON_OPTIONS);
            searchResult = buildSearchResult(search,searchParam);
            // 封装响应数据成我们需要的格式
        } catch (IOException e) {
            e.printStackTrace();
        }
        return searchResult;
    }

    /**
     * 构建结果数据
     *
     * @param search
     * @param searchParam
     * @return
     */
    private SearchResult buildSearchResult(SearchResponse search, SearchParam searchParam) {
        SearchResult searchResult = new SearchResult();
        

        // 1 返回所有查询出的商品
        SearchHits hits = search.getHits();
        if (hits.getHits() != null && hits.getHits().length > 0) {
            List<SkuEsModel> skuEsModels = new ArrayList<>();
            for (SearchHit hit : hits.getHits()) {
                String sourceAsString = hit.getSourceAsString();
                SkuEsModel skuEsModel = JSON.parseObject(sourceAsString, SkuEsModel.class);
                skuEsModels.add(skuEsModel);
            }
            searchResult.setProducts(skuEsModels);
        }

        //2 当前所有商品涉及到的所有属性
        List<SearchResult.AttrVo> attrVos = new ArrayList<>();

        ParsedNested attrAgg = search.getAggregations().get("attr_agg");

        ParsedLongTerms attrIdAgg = attrAgg.getAggregations().get("attr_id_agg");

        List<? extends Terms.Bucket> attrAggBuckets = attrIdAgg.getBuckets();
        for (Terms.Bucket bucket : attrAggBuckets) {
            SearchResult.AttrVo attrVo = new SearchResult.AttrVo();

            long attrId = bucket.getKeyAsNumber().longValue();
            attrVo.setAttrId(attrId);
            // 子聚合 属性的名字
            ParsedStringTerms attrNameAgg = bucket.getAggregations().get("attr_name_agg");
            String attrName = attrNameAgg.getBuckets().get(0).getKeyAsString();
            attrVo.setAttrName(attrName);

            // 子聚合 属性的值
            ParsedLongTerms attrValueAgg = bucket.getAggregations().get("attr_value_agg");
            List<String> attrValue = attrValueAgg.getBuckets().stream().map(item -> item.getKeyAsString()).collect(Collectors.toList());
            attrVo.setAttrValue(attrValue);
            attrVos.add(attrVo);
        }

        // 3当前所有商品的品牌信息
        List<SearchResult.BrandVo> brandVos = new ArrayList<>();
        ParsedLongTerms brandAgg = search.getAggregations().get("brand_agg");
        List<? extends Terms.Bucket> brandBuckets = brandAgg.getBuckets();
        for (Terms.Bucket brandBucket : brandBuckets) {
            SearchResult.BrandVo brandVo = new SearchResult.BrandVo();
            //  获取品牌Id
            long brandId = brandBucket.getKeyAsNumber().longValue();
            brandVo.setBrandId(brandId);
            // 子聚合 获取回应的 品牌名
            ParsedStringTerms brandNameAgg = brandBucket.getAggregations().get("brand_name_agg");
            String brandName = brandNameAgg.getBuckets().get(0).getKeyAsString();
            brandVo.setBrandName(brandName);

            // 子聚合 获取相应的 图片（logo）
            ParsedStringTerms  brandImgAgg = brandBucket.getAggregations().get("brand_img_agg");
            String brandImg = brandImgAgg.getBuckets().get(0).getKeyAsString();
            brandVo.setBrandImg(brandImg);

            brandVos.add(brandVo);
        }


        //4 当前所有商品涉及到的所有的分类信息
        ParsedLongTerms catalogAgg = search.getAggregations().get("catalog_agg");

        List<SearchResult.CatalogVo> catalogVos = new ArrayList<>();
        List<? extends Terms.Bucket> buckets = catalogAgg.getBuckets();

        for (Terms.Bucket bucket : buckets) {
            SearchResult.CatalogVo catalogVo = new SearchResult.CatalogVo();

            Long keyAsString = bucket.getKeyAsNumber().longValue();
            //  得到分类id
            catalogVo.setCatalogId(keyAsString);
            //  子聚合  // 得到分类名
            ParsedStringTerms aggregations = bucket.getAggregations().get("catalog_name_agg");
            String catalogName = aggregations.getBuckets().get(0).getKeyAsString();
            catalogVo.setCatalogName(catalogName);
            catalogVos.add(catalogVo);
        }


        //5 ,分页信息，总记录树
        long total = hits.getTotalHits().value;
        searchResult.setTotal(total);
        // 分页信息 总页码 计算
        int totalPage = (int)total % EsConstant.PRODUCT_PAGESIZE == 0 ? (int)total / EsConstant.PRODUCT_PAGESIZE : (int)(total / EsConstant.PRODUCT_PAGESIZE + 1);
        searchResult.setTotalPages(totalPage);
        // 页码
        searchResult.setPageNum(searchParam.getPageNum());

        List<Integer> pageNavs = new ArrayList<>();
        for (int i = 1; i <= totalPage; i++) {
            pageNavs.add(i);
        }
        searchResult.setPageNavs(pageNavs);

        return searchResult;
    }

    /**
     * 构建结果请求
     * # 模糊匹配，，过滤（按照属性，分类，品牌，价格区间，库存），排序，分页，高亮，聚合分析
     *
     * @param searchParam
     * @return
     */
    private SearchRequest buildSearchRequest(SearchParam searchParam) {


        // 构建DSL 语句
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

//        模糊匹配，，过滤（按照属性，分类，品牌，价格区间，库存）
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        // must -- 模糊匹配
        if (!StringUtils.isEmpty(searchParam.getKeyword())) {
            boolQuery.must(QueryBuilders.matchQuery("skuTitle", searchParam.getKeyword()));
        }

        // bool - filter  三级分类查询
        if (searchParam.getCatalog3Id() != null) {
            boolQuery.filter(QueryBuilders.termQuery("catalogId", searchParam.getCatalog3Id()));
        }
        // bool - filter  三级分类查询 按照品牌id 查询
        if (searchParam.getBrandId() != null && searchParam.getBrandId().size() > 0) {
            boolQuery.filter(QueryBuilders.termsQuery("brandId", searchParam.getBrandId()));
        }

        // // bool - filter  指定属性进行查询
        if (searchParam.getAttrs() != null && searchParam.getAttrs().size() > 0) {


            for (String attr : searchParam.getAttrs()) {
                BoolQueryBuilder nestedBoolQuery = QueryBuilders.boolQuery();
                // attrs=1_5寸:8寸&attrs=2_16g:5g
                String[] s = attr.split("_");
                //  属性id
                String attrId = s[0];
                // 属性检索值
                String[] attrValue = s[1].split(":");
                nestedBoolQuery.must(QueryBuilders.termQuery("attrs.attrId", attrId));
                nestedBoolQuery.must(QueryBuilders.termsQuery("attrs.attrValue", attrValue));
                // 每一个查询都必须生成一个nested 查询
                NestedQueryBuilder nestedQuery = QueryBuilders.nestedQuery("attrs", nestedBoolQuery, ScoreMode.None);
                boolQuery.filter(nestedQuery);
            }

        }

        // bool - filter   查询是否有库存
        boolQuery.filter(QueryBuilders.termQuery("hasStock", searchParam.getHasStock()));

        // bool - filter   按照价格区间
        if (!StringUtils.isEmpty(searchParam.getSkuPrice())) {
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("skuPrice");
            String [] skuPrice = searchParam.getSkuPrice().split("_");

            if (skuPrice.length == 2) {
                // 区间
                rangeQuery.gte(skuPrice[0]).lte(skuPrice[1]);
            } else if (skuPrice.length == 1) {
                if (searchParam.getSkuPrice().startsWith("_")) {
                    rangeQuery.lte(skuPrice[0]);
                }
                if (searchParam.getSkuPrice().endsWith("_")) {
                    rangeQuery.gte(skuPrice[0]);
                }
            }

            boolQuery.filter(rangeQuery);
        }

        sourceBuilder.query(boolQuery);

        //排序
        if (StringUtils.isNotEmpty(searchParam.getSort())) {
            String paramSort = searchParam.getSort();

            String[] s = paramSort.split("_");
            SortOrder sort = s[1].equalsIgnoreCase("asc")? SortOrder.ASC:SortOrder.DESC;
            sourceBuilder.sort(s[0],sort);
        }
        // 分页
        int pageNum = (searchParam.getPageNum() - 1) * EsConstant.PRODUCT_PAGESIZE;
        sourceBuilder.from(pageNum);
        sourceBuilder.size(EsConstant.PRODUCT_PAGESIZE);

        // 高亮
        if (StringUtils.isNotEmpty(searchParam.getKeyword())) {
            HighlightBuilder highlight = new HighlightBuilder();
            // 需要高亮的字段
            highlight.field("skuTitle");
            // 前置标签
            highlight.preTags("<b style='color:red;'>");
            highlight.postTags("</b>");
            sourceBuilder.highlighter(highlight);
        }

        /**
         * 聚合分析
         */
        // 品牌聚合
        TermsAggregationBuilder brandAgg = AggregationBuilders.terms("brand_agg");
        //size 查询50个数据
        brandAgg.field("brandId").size(50);

        //品牌聚合 子聚合
        brandAgg.subAggregation(AggregationBuilders.terms("brand_name_agg").field("brandName ").size(1));
        brandAgg.subAggregation(AggregationBuilders.terms("brand_img_agg").field("brandName ").size(1));
        sourceBuilder.aggregation(brandAgg);

        // 分类聚合
        TermsAggregationBuilder catalogAgg = AggregationBuilders.terms("catalog_agg").field("catalogId").size(20);
        catalogAgg.subAggregation(AggregationBuilders.terms("catalog_name_agg").field("catalogName").size(1));
        sourceBuilder.aggregation(catalogAgg);


        // 属性聚合
        NestedAggregationBuilder attrAgg = AggregationBuilders.nested("attr_agg", "attrs");

        //聚合出所有的的attrId
        TermsAggregationBuilder attrIdAgg = AggregationBuilders.terms("attr_id_agg").field("attrs.attrId");
        // attrId 对应的名字
        attrIdAgg.subAggregation(AggregationBuilders.terms("attr_name_agg").field("attrs.attrName").size(1));
        // attrId 对应的所有可能属性值
        attrIdAgg.subAggregation(AggregationBuilders.terms("attr_value_agg").field("attrs.attrValue").size(1));
        attrAgg.subAggregation(attrIdAgg);
        sourceBuilder.aggregation(attrAgg);

        SearchRequest searchRequest = new SearchRequest(new String[]{EsConstant.PRODUCT_INDEX}, sourceBuilder);

        return searchRequest;
    }
}
