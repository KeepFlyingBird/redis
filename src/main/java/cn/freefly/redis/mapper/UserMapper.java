package cn.freefly.redis.mapper;

import cn.freefly.redis.model.User;
import org.springframework.stereotype.Repository;

/**
 * @Description:
 * @author: xhzl.xiaoyunfei
 * @date: 2021.09.16
 */
public interface UserMapper {
    User findUserInfo();
}
