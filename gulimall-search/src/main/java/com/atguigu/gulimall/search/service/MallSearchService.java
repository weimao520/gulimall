package com.atguigu.gulimall.search.service;

import com.atguigu.gulimall.search.vo.SearchParam;
import com.atguigu.gulimall.search.vo.SearchResult;

/**
 * @author WeiMao
 * @create 2021-06-24 17:07
 */
public interface MallSearchService {

    /**
     * 检索的所有参数
     * @param searchParam
     * @return
     */
    SearchResult search(SearchParam searchParam);
}
