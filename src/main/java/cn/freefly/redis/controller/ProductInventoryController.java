package cn.freefly.redis.controller;

import cn.freefly.redis.model.ProductInventory;
import cn.freefly.redis.request.ProductInventoryCacheRefreshRequest;
import cn.freefly.redis.request.ProductInventoryDBUpdateRequest;
import cn.freefly.redis.request.Request;
import cn.freefly.redis.service.ProductInventoryService;
import cn.freefly.redis.service.RequestAsyncProcessService;
import cn.freefly.redis.vo.Response;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;

/**
 * @Description: 商品库存Controller
 * @author: xhzl.xiaoyunfei
 * @date: 2021.09.17
 *
 * 大家考虑一下，我要模拟的场景：
 *（1）一个更新商品库存的请求过来，然后此时会先删除redis中的缓存，然后模拟卡顿5秒钟
 *（2）在这个卡顿的5秒钟内，我们发送一个商品缓存的读请求，因为此时redis中没有缓存，就会来请求将数据库中最新的数据刷新到缓存中
 *（3）此时读请求会路由到同一个内存队列中，阻塞住，不会执行
 *（4）等5秒钟过后，写请求完成了数据库的更新之后，读请求才会执行
 *（5）读请求执行的时候，会将最新的库存从数据库中查询出来，然后更新到缓存中
 *
 * 如果是不一致的情况，可能会出现redis中还是库存为100，但是数据库中也许已经更新成了库存为99了
 * 现在做了一致性保障的方案之后，就可以保证说，数据是一致的
 */
@Controller
public class ProductInventoryController {
    @Resource
    private RequestAsyncProcessService requestAsyncProcessService;
    @Resource
    private ProductInventoryService productInventoryService;

    /**
     * 更新商品库存
     */
    @RequestMapping("/updateProductInventory")
    @ResponseBody
    public Response updateProductInventory(ProductInventory productInventory) {
        // 为了简单起见，我们就不用log4j那种日志框架去打印日志了
        // 其实log4j也很简单，实际企业中都是用log4j去打印日志的，自己百度一下
        System.out.println("===========日志===========: 接收到更新商品库存的请求，商品id=" + productInventory.getProductId() + ", 商品库存数量=" + productInventory.getInventoryCnt());

        Response response = null;

        try {
            Request request = new ProductInventoryDBUpdateRequest(
                    productInventory, productInventoryService);
            requestAsyncProcessService.process(request);
            response = new Response(Response.SUCCESS);
        } catch (Exception e) {
            e.printStackTrace();
            response = new Response(Response.FAILURE);
        }

        return response;
    }

    /**
     * 获取商品库存
     */
    @RequestMapping("/getProductInventory")
    @ResponseBody
    public ProductInventory getProductInventory(Integer productId) {
        System.out.println("===========日志===========: 接收到一个商品库存的读请求，商品id=" + productId);

        ProductInventory productInventory = null;

        try {
            Request request = new ProductInventoryCacheRefreshRequest(
                    productId, productInventoryService,false);
            requestAsyncProcessService.process(request);

            // 将请求扔给service异步去处理以后，就需要while(true)一会儿，在这里hang住
            // 去尝试等待前面有商品库存更新的操作，同时缓存刷新的操作，将最新的数据刷新到缓存中
            long startTime = System.currentTimeMillis();
            long endTime = 0L;
            long waitTime = 0L;

            // 等待超过200ms没有从缓存中获取到结果
            while(true) {
//				if(waitTime > 25000) {
//					break;
//				}

                // 一般公司里面，面向用户的读请求控制在200ms就可以了
                if(waitTime > 200) {
                    break;
                }

                // 尝试去redis中读取一次商品库存的缓存数据
                productInventory = productInventoryService.getProductInventoryCache(productId);

                // 如果读取到了结果，那么就返回
                if(productInventory != null) {
                    System.out.println("===========日志===========: 在200ms内读取到了redis中的库存缓存，商品id=" + productInventory.getProductId() + ", 商品库存数量=" + productInventory.getInventoryCnt());
                    return productInventory;
                }

                // 如果没有读取到结果，那么等待一段时间
                else {
                    Thread.sleep(20);
                    endTime = System.currentTimeMillis();
                    waitTime = endTime - startTime;
                }
            }

            // 代码运行到这里直接尝试从数据库中读取数据，只有三种情况
            // 1、就是说，上一次也是读请求，数据刷入了redis,但是redis LRU算法给清理掉了，标志位还是false
            // 所以此时下一个读请求是从缓存中是拿不到数据的，再放一个读request进队列，让数据去刷新一下
            // 2、可能在200ms内，就是读请求在队列中一直积压着，没有等待到它执行（在实际生产中，基本上是比较坑了，扩容机器或者数据库查询优化性能）
            // 所以就直接查一次库，然后给队列里塞进去一个刷新缓存的请求
            // 3、数据库里本身就没有，缓存穿透，穿透redis，请求到达mysql数据库
            productInventory = productInventoryService.findProductInventory(productId);
            request = new ProductInventoryCacheRefreshRequest(
                    productId, productInventoryService,true);
            requestAsyncProcessService.process(request);
            return productInventory;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ProductInventory(productId, -1L);
    }
}
