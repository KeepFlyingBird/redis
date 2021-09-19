package cn.freefly.redis.service;

import cn.freefly.redis.model.User;

public interface UserService {
    User findUserInfo();
    User getCaseUserInfo();
}
