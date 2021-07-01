package com.atguigu.gulimall.search.vo;

import com.atguigu.common.to.es.SkuEsModel;
import lombok.Data;

import java.util.List;

/**
 * @author WeiMao
 * @create 2021-06-26 11:13
 */
@Data
public class SearchResult {

    private List<SkuEsModel> products;

    /**
     * 当前页码
     */
    private Integer pageNum;

    /**
     * 导航页码
     */
    private List<Integer> pageNavs;

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 总页码
     */
    private Integer totalPages;

    /**
     * 品牌信息
     */
    private List<BrandVo> brands;

    /**
     * 当前查询的所有属性
     */
    private List<AttrVo> attrs;

    /**
     * 当前查询的所有分類
     */
    private List<CatalogVo> catalogs;

    @Data
    public static class BrandVo{

        private Long brandId;
        private String brandName;

        private String brandImg;
    }

    @Data
    public static class AttrVo{

        private Long attrId;

        private String attrName;

        private List<String> attrValue;
    }

    @Data
    public static class CatalogVo {

        private Long catalogId;

        private String catalogName;


    }

}
