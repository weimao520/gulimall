package com.atguigu.gulimall.search.vo;

import lombok.Data;

import java.util.List;

/**
 * 将页面所有可以传递过来的查询条件进行封装
 * @author WeiMao
 * @create 2021-06-24 17:05
 */
@Data
public class SearchParam {

    /**
     * 关键字 全文检索
     */
    private String keyword;

    /**
     * 三级分类Id
     */
    private Long catalog3Id;

    /**
     * 排序条件： saleCount_asc/desc 销售属性, shuPrice_asc/desc 价格排序，hostScore_asc/desc  综合（热度，评分）排序
     */
    private String sort;

    /**
     * 是否显示有货
     */
    private Integer hasStock;

    /**
     * 价格区间
     */
    private String skuPrice;

    /**
     * 按照品牌查询 可以多选
     */
    private List<Long> brandId;

    /**
     * 按照属性进行多选
     */
    private List<String> attrs;

    /**
     * 页码 第几页
     */
    private Integer pageNum =1;
}
