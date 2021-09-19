package cn.freefly.redis.controller;

import cn.freefly.redis.model.User;
import cn.freefly.redis.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @Description:
 * @author: xhzl.xiaoyunfei
 * @date: 2021.09.16
 */
@Controller
public class UserController {
    @Autowired
    private UserService userService;

    @RequestMapping("/getUserInfo")
    @ResponseBody
    public User getUserInfo() {
        User user = userService.findUserInfo();
        return user;
    }


    @RequestMapping("/getCaseUserInfo")
    @ResponseBody
    public User getCaseUserInfo() {
        User user = userService.getCaseUserInfo();
        return user;
    }
}
