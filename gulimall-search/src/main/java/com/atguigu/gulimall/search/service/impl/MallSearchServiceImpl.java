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

        // ??????????????????
        SearchRequest searchRequest = buildSearchRequest(searchParam);
        try {
            // ??????????????????
            SearchResponse search = this.client.search(searchRequest, GulimallElasticSeachConfig.COMMON_OPTIONS);
            searchResult = buildSearchResult(search,searchParam);
            // ??????????????????????????????????????????
        } catch (IOException e) {
            e.printStackTrace();
        }
        return searchResult;
    }

    /**
     * ??????????????????
     *
     * @param search
     * @param searchParam
     * @return
     */
    private SearchResult buildSearchResult(SearchResponse search, SearchParam searchParam) {
        SearchResult searchResult = new SearchResult();
        

        // 1 ??????????????????????????????
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

        //2 ??????????????????????????????????????????
        List<SearchResult.AttrVo> attrVos = new ArrayList<>();

        ParsedNested attrAgg = search.getAggregations().get("attr_agg");

        ParsedLongTerms attrIdAgg = attrAgg.getAggregations().get("attr_id_agg");

        List<? extends Terms.Bucket> attrAggBuckets = attrIdAgg.getBuckets();
        for (Terms.Bucket bucket : attrAggBuckets) {
            SearchResult.AttrVo attrVo = new SearchResult.AttrVo();

            long attrId = bucket.getKeyAsNumber().longValue();
            attrVo.setAttrId(attrId);
            // ????????? ???????????????
            ParsedStringTerms attrNameAgg = bucket.getAggregations().get("attr_name_agg");
            String attrName = attrNameAgg.getBuckets().get(0).getKeyAsString();
            attrVo.setAttrName(attrName);

            // ????????? ????????????
            ParsedLongTerms attrValueAgg = bucket.getAggregations().get("attr_value_agg");
            List<String> attrValue = attrValueAgg.getBuckets().stream().map(item -> item.getKeyAsString()).collect(Collectors.toList());
            attrVo.setAttrValue(attrValue);
            attrVos.add(attrVo);
        }

        // 3?????????????????????????????????
        List<SearchResult.BrandVo> brandVos = new ArrayList<>();
        ParsedLongTerms brandAgg = search.getAggregations().get("brand_agg");
        List<? extends Terms.Bucket> brandBuckets = brandAgg.getBuckets();
        for (Terms.Bucket brandBucket : brandBuckets) {
            SearchResult.BrandVo brandVo = new SearchResult.BrandVo();
            //  ????????????Id
            long brandId = brandBucket.getKeyAsNumber().longValue();
            brandVo.setBrandId(brandId);
            // ????????? ??????????????? ?????????
            ParsedStringTerms brandNameAgg = brandBucket.getAggregations().get("brand_name_agg");
            String brandName = brandNameAgg.getBuckets().get(0).getKeyAsString();
            brandVo.setBrandName(brandName);

            // ????????? ??????????????? ?????????logo???
            ParsedStringTerms  brandImgAgg = brandBucket.getAggregations().get("brand_img_agg");
            String brandImg = brandImgAgg.getBuckets().get(0).getKeyAsString();
            brandVo.setBrandImg(brandImg);

            brandVos.add(brandVo);
        }


        //4 ???????????????????????????????????????????????????
        ParsedLongTerms catalogAgg = search.getAggregations().get("catalog_agg");

        List<SearchResult.CatalogVo> catalogVos = new ArrayList<>();
        List<? extends Terms.Bucket> buckets = catalogAgg.getBuckets();

        for (Terms.Bucket bucket : buckets) {
            SearchResult.CatalogVo catalogVo = new SearchResult.CatalogVo();

            Long keyAsString = bucket.getKeyAsNumber().longValue();
            //  ????????????id
            catalogVo.setCatalogId(keyAsString);
            //  ?????????  // ???????????????
            ParsedStringTerms aggregations = bucket.getAggregations().get("catalog_name_agg");
            String catalogName = aggregations.getBuckets().get(0).getKeyAsString();
            catalogVo.setCatalogName(catalogName);
            catalogVos.add(catalogVo);
        }


        //5 ,???????????????????????????
        long total = hits.getTotalHits().value;
        searchResult.setTotal(total);
        // ???????????? ????????? ??????
        int totalPage = (int)total % EsConstant.PRODUCT_PAGESIZE == 0 ? (int)total / EsConstant.PRODUCT_PAGESIZE : (int)(total / EsConstant.PRODUCT_PAGESIZE + 1);
        searchResult.setTotalPages(totalPage);
        // ??????
        searchResult.setPageNum(searchParam.getPageNum());

        List<Integer> pageNavs = new ArrayList<>();
        for (int i = 1; i <= totalPage; i++) {
            pageNavs.add(i);
        }
        searchResult.setPageNavs(pageNavs);

        return searchResult;
    }

    /**
     * ??????????????????
     * # ??????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
     *
     * @param searchParam
     * @return
     */
    private SearchRequest buildSearchRequest(SearchParam searchParam) {


        // ??????DSL ??????
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

//        ????????????????????????????????????????????????????????????????????????????????????
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        // must -- ????????????
        if (!StringUtils.isEmpty(searchParam.getKeyword())) {
            boolQuery.must(QueryBuilders.matchQuery("skuTitle", searchParam.getKeyword()));
        }

        // bool - filter  ??????????????????
        if (searchParam.getCatalog3Id() != null) {
            boolQuery.filter(QueryBuilders.termQuery("catalogId", searchParam.getCatalog3Id()));
        }
        // bool - filter  ?????????????????? ????????????id ??????
        if (searchParam.getBrandId() != null && searchParam.getBrandId().size() > 0) {
            boolQuery.filter(QueryBuilders.termsQuery("brandId", searchParam.getBrandId()));
        }

        // // bool - filter  ????????????????????????
        if (searchParam.getAttrs() != null && searchParam.getAttrs().size() > 0) {


            for (String attr : searchParam.getAttrs()) {
                BoolQueryBuilder nestedBoolQuery = QueryBuilders.boolQuery();
                // attrs=1_5???:8???&attrs=2_16g:5g
                String[] s = attr.split("_");
                //  ??????id
                String attrId = s[0];
                // ???????????????
                String[] attrValue = s[1].split(":");
                nestedBoolQuery.must(QueryBuilders.termQuery("attrs.attrId", attrId));
                nestedBoolQuery.must(QueryBuilders.termsQuery("attrs.attrValue", attrValue));
                // ????????????????????????????????????nested ??????
                NestedQueryBuilder nestedQuery = QueryBuilders.nestedQuery("attrs", nestedBoolQuery, ScoreMode.None);
                boolQuery.filter(nestedQuery);
            }

        }

        // bool - filter   ?????????????????????
        boolQuery.filter(QueryBuilders.termQuery("hasStock", searchParam.getHasStock()));

        // bool - filter   ??????????????????
        if (!StringUtils.isEmpty(searchParam.getSkuPrice())) {
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("skuPrice");
            String [] skuPrice = searchParam.getSkuPrice().split("_");

            if (skuPrice.length == 2) {
                // ??????
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

        //??????
        if (StringUtils.isNotEmpty(searchParam.getSort())) {
            String paramSort = searchParam.getSort();

            String[] s = paramSort.split("_");
            SortOrder sort = s[1].equalsIgnoreCase("asc")? SortOrder.ASC:SortOrder.DESC;
            sourceBuilder.sort(s[0],sort);
        }
        // ??????
        int pageNum = (searchParam.getPageNum() - 1) * EsConstant.PRODUCT_PAGESIZE;
        sourceBuilder.from(pageNum);
        sourceBuilder.size(EsConstant.PRODUCT_PAGESIZE);

        // ??????
        if (StringUtils.isNotEmpty(searchParam.getKeyword())) {
            HighlightBuilder highlight = new HighlightBuilder();
            // ?????????????????????
            highlight.field("skuTitle");
            // ????????????
            highlight.preTags("<b style='color:red;'>");
            highlight.postTags("</b>");
            sourceBuilder.highlighter(highlight);
        }

        /**
         * ????????????
         */
        // ????????????
        TermsAggregationBuilder brandAgg = AggregationBuilders.terms("brand_agg");
        //size ??????50?????????
        brandAgg.field("brandId").size(50);

        //???????????? ?????????
        brandAgg.subAggregation(AggregationBuilders.terms("brand_name_agg").field("brandName ").size(1));
        brandAgg.subAggregation(AggregationBuilders.terms("brand_img_agg").field("brandName ").size(1));
        sourceBuilder.aggregation(brandAgg);

        // ????????????
        TermsAggregationBuilder catalogAgg = AggregationBuilders.terms("catalog_agg").field("catalogId").size(20);
        catalogAgg.subAggregation(AggregationBuilders.terms("catalog_name_agg").field("catalogName").size(1));
        sourceBuilder.aggregation(catalogAgg);


        // ????????????
        NestedAggregationBuilder attrAgg = AggregationBuilders.nested("attr_agg", "attrs");

        //?????????????????????attrId
        TermsAggregationBuilder attrIdAgg = AggregationBuilders.terms("attr_id_agg").field("attrs.attrId");
        // attrId ???????????????
        attrIdAgg.subAggregation(AggregationBuilders.terms("attr_name_agg").field("attrs.attrName").size(1));
        // attrId ??????????????????????????????
        attrIdAgg.subAggregation(AggregationBuilders.terms("attr_value_agg").field("attrs.attrValue").size(1));
        attrAgg.subAggregation(attrIdAgg);
        sourceBuilder.aggregation(attrAgg);

        SearchRequest searchRequest = new SearchRequest(new String[]{EsConstant.PRODUCT_INDEX}, sourceBuilder);

        return searchRequest;
    }
}
