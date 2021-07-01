package com.atguigu.gulimall.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.gulimall.product.service.CategoryBrandRelationService;
import com.atguigu.gulimall.product.service.CategoryService;
import com.atguigu.gulimall.product.vo.Catelog2Vo;
import org.apache.commons.lang3.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.product.dao.CategoryDao;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    //    @Autowired
//    CategoryDao categoryDao;
    @Autowired
    StringRedisTemplate restTemplate;


    @Autowired
    CategoryBrandRelationService categoryBrandRelationService;


    @Autowired
    RedissonClient redisson;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {

        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<CategoryEntity> listWithTree() {
        //1、查出所有分类
        List<CategoryEntity> entities = baseMapper.selectList(null);

        //2、组装成父子的树形结构

        //2.1）、找到所有的一级分类
        List<CategoryEntity> level1Menus = entities.stream().filter(categoryEntity ->
                categoryEntity.getParentCid() == 0
        ).map((menu) -> {
            menu.setChildren(getChildrens(menu, entities));
            return menu;
        }).sorted((menu1, menu2) -> {
            return (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort());
        }).collect(Collectors.toList());


        return level1Menus;
    }

    @Override
    public void removeMenuByIds(List<Long> asList) {
        //TODO  1、检查当前删除的菜单，是否被别的地方引用

        //逻辑删除
        baseMapper.deleteBatchIds(asList);
    }

    //[2,25,225]
    @Override
    public Long[] findCatelogPath(Long catelogId) {
        List<Long> paths = new ArrayList<>();
        List<Long> parentPath = findParentPath(catelogId, paths);

        Collections.reverse(parentPath);


        return parentPath.toArray(new Long[parentPath.size()]);
    }

    /**
     * 级联更新所有关联的数据
     *
     * @param category
     */
    @CacheEvict(value = "category",key = "'getCatalogJson'")
    @Transactional
    @Override
    public void updateCascade(CategoryEntity category) {
        this.updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(), category.getName());
    }

    @Override
    @Cacheable(value = "category",key = "#root.methodName")
    public List<CategoryEntity> getLevel1Categorys() {

        QueryWrapper<CategoryEntity> queryWrapper = new QueryWrapper<>();

        queryWrapper.lambda()
                .eq(CategoryEntity::getParentCid, 0);

        List<CategoryEntity> categoryEntities = this.baseMapper.selectList(queryWrapper);

        return categoryEntities;
    }



    @Override
    @Cacheable(value = "category",key = "#root.method.name")
    public Map<String, List<Catelog2Vo>> getCatalogJson(){
        System.out.println("查询了数据库");

        //将数据库的多次查询变为一次
        List<CategoryEntity> selectList = this.baseMapper.selectList(null);

        //1、查出所有分类
        //1、1）查出所有一级分类
        List<CategoryEntity> level1Categorys = getParent_cid(selectList, 0L);

        //封装数据
        Map<String, List<Catelog2Vo>> parentCid = level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            //1、每一个的一级分类,查到这个一级分类的二级分类
            List<CategoryEntity> categoryEntities = getParent_cid(selectList, v.getCatId());

            //2、封装上面的结果
            List<Catelog2Vo> catelog2Vos = null;
            if (categoryEntities != null) {
                catelog2Vos = categoryEntities.stream().map(l2 -> {
                    Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName().toString());

                    //1、找当前二级分类的三级分类封装成vo
                    List<CategoryEntity> level3Catelog = getParent_cid(selectList, l2.getCatId());

                    if (level3Catelog != null) {
                        List<Catelog2Vo.Category3Vo> category3Vos = level3Catelog.stream().map(l3 -> {
                            //2、封装成指定格式
                            Catelog2Vo.Category3Vo category3Vo = new Catelog2Vo.Category3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());

                            return category3Vo;
                        }).collect(Collectors.toList());
                        catelog2Vo.setCatalog3List(category3Vos);
                    }

                    return catelog2Vo;
                }).collect(Collectors.toList());
            }

            return catelog2Vos;
        }));

        return parentCid;
    }



//    // todo  堆外 内存溢出
//
//    @Override
//    public Map<String, List<Catelog2Vo>> getCatalogJson2() {
//        // 缓存中存的都是json字符串
//        String catalogJSON = restTemplate.opsForValue().get("catalogJSON");
//        /**
//         * 空结果缓存，解决缓存穿透
//         *  设置随机过期时间，解决 缓存雪崩
//         *      加锁，解决缓存穿透
//         */
//        //json 是跨语言平台的
//        // 判斷是否缓存中是否存在
//        if (StringUtils.isEmpty(catalogJSON)) {
//            // 加入缓存
//            Map<String, List<Catelog2Vo>> catalogJsonFromDB = getCatalogJsonFromDBWithLocalLock();
//
//            String s = JSONObject.toJSONString(catalogJsonFromDB);
//
//            this.restTemplate.opsForValue().set("catalogJSON", s, 1, TimeUnit.DAYS);
//
//        }
//
//        Map<String, List<Catelog2Vo>> result = JSON.parseObject(catalogJSON, new TypeReference<Map<String, List<Catelog2Vo>>>() {
//        });
//
//        return result;
//
//    }


    /**
     * 分布式锁 最终解决方案 redission
     *
     * @return
     */
    public Map<String, List<Catelog2Vo>> getCatalogJsonFromDBWithRedissionLock() {


        // 1,锁的名字，锁的粒度，越细越好
        RLock lock = redisson.getLock("CatalogJson-lock");
        //上锁
        lock.lock();


        // 查看这把锁是否存在，不存在就加锁
        // 加锁成功
        Map<String, List<Catelog2Vo>> dataFromOb = null;
        try {
            // 防止死锁，加入过期时间
            dataFromOb = getDataFromOb();
        } finally {
            lock.unlock();
        }


        return dataFromOb;


    }

    /**
     * 分布式锁
     *
     * @return
     */
    public Map<String, List<Catelog2Vo>> getCatalogJsonFromDBWithRedisLock() {


        // 查看这把锁是否存在，不存在就加锁
        String uuid = UUID.randomUUID().toString();
        // 原子操作
        Boolean lock = this.restTemplate.opsForValue().setIfAbsent("lock", "111", 300, TimeUnit.SECONDS);
        // 加锁成功
        if (lock) {
            Map<String, List<Catelog2Vo>> dataFromOb = null;
            try {
                // 防止死锁，加入过期时间
                dataFromOb = getDataFromOb();
            } finally {

                // 释放锁 (删除锁)
//            String lockValue = this.restTemplate.opsForValue().get("lock");
                // 只能删除自己的锁
                //  因为有个 获取redis 数据的过程，不确定要耗时多少，不能保证原子性，所以获取值跟删除值都交给redis 自己完成
//            if (uuid.equals(lockValue)) {
//                this.restTemplate.delete("lock");
//            }

                // redis 脚本
                String script = "if redis.call(\"get\",KEYS[1]) == ARGV[1]\n" +
                        "then\n" +
                        "    return redis.call(\"del\",KEYS[1])\n" +
                        "else\n" +
                        "    return 0\n" +
                        "end";
                // 原子 删除锁
                this.restTemplate.execute(new DefaultRedisScript<>(script, Long.class), Arrays.asList("lock"), uuid);
            }


            return dataFromOb;
        } else {
            //加锁失败 重试
            return getCatalogJsonFromDBWithRedisLock();
        }

    }

    /**
     * 本地锁
     *
     * @return
     */
    public Map<String, List<Catelog2Vo>> getCatalogJsonFromDBWithLocalLock() {


        synchronized (this) {

            return getDataFromOb();
        }


    }


    private Map<String, List<Catelog2Vo>> getDataFromOb() {
        String catalogJSON = restTemplate.opsForValue().get("catalogJSON");
        if (!StringUtils.isEmpty(catalogJSON)) {

            Map<String, List<Catelog2Vo>> result = JSON.parseObject(catalogJSON, new TypeReference<Map<String, List<Catelog2Vo>>>() {
            });

            return result;
        }

        System.out.println("查询了数据库");

        //将数据库的多次查询变为一次
        List<CategoryEntity> selectList = this.baseMapper.selectList(null);

        //1、查出所有分类
        //1、1）查出所有一级分类
        List<CategoryEntity> level1Categorys = getParent_cid(selectList, 0L);

        //封装数据
        Map<String, List<Catelog2Vo>> parentCid = level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            //1、每一个的一级分类,查到这个一级分类的二级分类
            List<CategoryEntity> categoryEntities = getParent_cid(selectList, v.getCatId());

            //2、封装上面的结果
            List<Catelog2Vo> catelog2Vos = null;
            if (categoryEntities != null) {
                catelog2Vos = categoryEntities.stream().map(l2 -> {
                    Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName().toString());

                    //1、找当前二级分类的三级分类封装成vo
                    List<CategoryEntity> level3Catelog = getParent_cid(selectList, l2.getCatId());

                    if (level3Catelog != null) {
                        List<Catelog2Vo.Category3Vo> category3Vos = level3Catelog.stream().map(l3 -> {
                            //2、封装成指定格式
                            Catelog2Vo.Category3Vo category3Vo = new Catelog2Vo.Category3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());

                            return category3Vo;
                        }).collect(Collectors.toList());
                        catelog2Vo.setCatalog3List(category3Vos);
                    }

                    return catelog2Vo;
                }).collect(Collectors.toList());
            }

            return catelog2Vos;
        }));

        String s = JSONObject.toJSONString(parentCid);

        this.restTemplate.opsForValue().set("catalogJSON", s, 1, TimeUnit.DAYS);
        return parentCid;
    }


    private List<CategoryEntity> getParent_cid(List<CategoryEntity> selectList, Long parentCid) {
        List<CategoryEntity> categoryEntities = selectList.stream().filter(item -> item.getParentCid().equals(parentCid)).collect(Collectors.toList());
        return categoryEntities;
        // return this.baseMapper.selectList(
        //         new QueryWrapper<CategoryEntity>().eq("parent_cid", parentCid));
    }

    //225,25,2
    private List<Long> findParentPath(Long catelogId, List<Long> paths) {
        //1、收集当前节点id
        paths.add(catelogId);
        CategoryEntity byId = this.getById(catelogId);
        if (byId.getParentCid() != 0) {
            findParentPath(byId.getParentCid(), paths);
        }
        return paths;

    }


    //递归查找所有菜单的子菜单
    private List<CategoryEntity> getChildrens(CategoryEntity root, List<CategoryEntity> all) {

        List<CategoryEntity> children = all.stream().filter(categoryEntity -> {
            return categoryEntity.getParentCid() == root.getCatId();
        }).map(categoryEntity -> {
            //1、找到子菜单
            categoryEntity.setChildren(getChildrens(categoryEntity, all));
            return categoryEntity;
        }).sorted((menu1, menu2) -> {
            //2、菜单的排序
            return (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort());
        }).collect(Collectors.toList());

        return children;
    }


}