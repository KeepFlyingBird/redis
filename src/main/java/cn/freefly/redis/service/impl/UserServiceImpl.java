package cn.freefly.redis.service.impl;

import cn.freefly.redis.mapper.UserMapper;
import cn.freefly.redis.model.User;
import cn.freefly.redis.service.UserService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.util.BeanUtil;
import com.fasterxml.jackson.databind.util.JSONPObject;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * @Description:
 * @author: xhzl.xiaoyunfei
 * @date: 2021.09.16
 */
@Service
public class UserServiceImpl implements UserService {
    @Resource
    private UserMapper userMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public User findUserInfo() {
        return userMapper.findUserInfo();
    }

    @Override
    public User getCaseUserInfo() {
        User user = new User();
        user.setName("yueyue");
        user.setAge(2);
        redisTemplate.opsForValue().set("caseUserYueyue",JSON.toJSONString(user),1000 * 30 , TimeUnit.SECONDS);
        String caseUserYueyue = String.valueOf(redisTemplate.opsForValue().get("caseUserYueyue"));
        JSONObject jsonObject = JSONObject.parseObject(caseUserYueyue);
        User user1 = new User();
        user1.setName(String.valueOf(jsonObject.get("name")));
        user1.setAge(Integer.parseInt(String.valueOf(jsonObject.get("age"))));
        return user1;
    }
}
