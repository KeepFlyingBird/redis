package cn.freefly.redis.service.impl;

import cn.freefly.redis.mapper.ProductInventoryMapper;
import cn.freefly.redis.model.ProductInventory;
import cn.freefly.redis.service.ProductInventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @Description:商品库存Service实现类
 * @author: xhzl.xiaoyunfei
 * @date: 2021.09.17
 */
@Service
public class ProductInventoryServiceImpl implements ProductInventoryService {
    @Resource
    private ProductInventoryMapper productInventoryMapper;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 更新数据库中库存
     * @param productInventory 商品库存
     */
    @Override
    public void updateProductInventory(ProductInventory productInventory) {
        productInventoryMapper.updateProductInventory(productInventory);
        System.out.println("===========日志===========: 已修改数据库中的库存，商品id=" + productInventory.getProductId() + ", 商品库存数量=" + productInventory.getInventoryCnt());
    }

    /**
     * 删除redis缓存
     * @param productInventory 商品库存
     */
    @Override
    public void removeProductInventoryCache(ProductInventory productInventory) {
        String key = "product:inventory:" + productInventory.getProductId();
        redisTemplate.delete(key);
        System.out.println("===========日志===========: 已删除redis中的缓存，key=" + key);
    }

    /**
     * 根据商品id查询商品库存
     * @param productId 商品id
     * @return
     */
    @Override
    public ProductInventory findProductInventory(Integer productId) {
        ProductInventory productInventory = productInventoryMapper.findProductInventory(productId);
        return productInventory == null ? new ProductInventory(productId,-1L) : productInventory;
    }

    /**
     * 设置商品库存的缓存
     * @param productInventory 商品库存
     */
    @Override
    public void setProductInventoryCache(ProductInventory productInventory) {
        String key = "product:inventory:" + productInventory.getProductId();
        redisTemplate.opsForValue().set(key, String.valueOf(productInventory.getInventoryCnt()));
        System.out.println("===========日志===========: 已更新商品库存的缓存，商品id=" + productInventory.getProductId() + ", 商品库存数量=" + productInventory.getInventoryCnt() + ", key=" + key);
    }

    /**
     * 获取商品库存的缓存
     * @param productId
     * @return
     */
    @Override
    public ProductInventory getProductInventoryCache(Integer productId) {
        Long inventoryCnt = 0L;

        String key = "product:inventory:" + productId;
        String result = String.valueOf(redisTemplate.opsForValue().get(key));
        if(result != null && !"".equals(result) && !"null".equals(result)) {
            try {
                inventoryCnt = Long.valueOf(result);
                return new ProductInventory(productId, inventoryCnt);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return null;
    }
}
