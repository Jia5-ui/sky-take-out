package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.sky.constant.MessageConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.exception.LoginFailedException;
import com.sky.exception.UserNotLoginException;
import com.sky.mapper.UserMapper;
import com.sky.properties.WeChatProperties;
import com.sky.service.UserService;
import com.sky.utils.HttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class UserServiceImpl implements UserService{
    //微信登录接口地址
    private final static String LOGIN_URL="https://api.weixin.qq.com/sns/jscode2session";
    @Autowired
    private WeChatProperties weChatProperties;
    @Autowired
    private UserMapper userMapper;

    /**
     * 微信登录
     * @param userLoginDTO
     * @return
     */
    public User wxLogin(UserLoginDTO userLoginDTO) {
        //调用微信接口，获取openid
        String openid = getOpenid(userLoginDTO.getCode());

        //判断openid是否为空？登陆失败抛业务异常
        if (openid == null) {
            //登陆失败
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }

        //判断是否是新用户，如果是新用户则自动注册
        User user = userMapper.getByOpenid(openid);

        if (user == null) {
            //新用户，自动注册
            user = User.builder()
                    .openid(openid)
                    .createTime(LocalDateTime.now())
                    .build();
            userMapper.insert(user);
        }


        //返回用户信息
        return user;
    }

    private String getOpenid(String code){
        //调用微信接口，获取session_key和openid
        Map<String, String> params = new HashMap<>();
        params.put("appid",weChatProperties.getAppid());
        params.put("secret",weChatProperties.getSecret());
        params.put("js_code",code);
        params.put("grant_type","authorization_code");
        //JSON字符串
        String json = HttpClientUtil.doGet(LOGIN_URL, params);
        //解析JSON字符串
        JSONObject jsonObject = JSONObject.parseObject(json);
        //获取openid
        String openid = jsonObject.getString("openid");

        return openid;
    }
}
