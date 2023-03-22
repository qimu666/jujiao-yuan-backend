package com.qimu.jujiao.job;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qimu.jujiao.model.entity.User;
import com.qimu.jujiao.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


/**
 * @Author: QiMu
 * @Date: 2023年03月12日 13:40
 * @Version: 1.0
 * @Description: 预热缓存, 让第一个用户查看页面速度也快
 */
@Component
@Slf4j
public class CacheWarming {
    @Resource
    private UserService userService;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    private List<Long> mainUserList = Arrays.asList(1L);

    // 每天的2点半更新缓存
    @Scheduled(cron = "0 30 2 * * *")
    public void searchList() {
        for (Long mainUserId : mainUserList) {
            QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
            // Page<User> userPage = userService.page(new Page<>(1, 10), userQueryWrapper);
            List<User> list = userService.list(userQueryWrapper);
            List<User> result = list.stream().map(user -> userService.getSafetyUser(user)).collect(Collectors.toList());
            try {
                redisTemplate.opsForValue().set(userService.redisFormat(mainUserId), result, 1 + RandomUtil.randomInt(1, 5), TimeUnit.MINUTES);
            } catch (Exception e) {
                log.error("redis set key error", e);
            }
        }
    }
}
