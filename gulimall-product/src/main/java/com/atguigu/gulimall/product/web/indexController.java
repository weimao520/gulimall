package com.atguigu.gulimall.product.web;

import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryService;
import com.atguigu.gulimall.product.vo.Catelog2Vo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

/**
 * @author WeiMao
 * @create 2021-06-02 11:04
 */
@Controller
public class indexController {

    @Autowired
    CategoryService categoryService;

    @GetMapping({"/","/index.html"})
    public String indexPage(Model model){

        // 拿到所有的1及分类
        List<CategoryEntity> level1Categorys = this.categoryService.getLevel1Categorys();

        model.addAttribute("categorys", level1Categorys);

        return "index";
    }


    @ResponseBody
    @GetMapping("/index/catalog.json")
    public Map<String,List<Catelog2Vo>> getCatalogJson(){

      Map<String,List<Catelog2Vo>> map =  this.categoryService.getCatalogJson();
        return map;
    }
}
