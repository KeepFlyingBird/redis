package cn.freefly.redis.service;

import cn.freefly.redis.request.Request;

/**
 * 请求异步执行的service
 */
public interface RequestAsyncProcessService {
    void process(Request request);
}
