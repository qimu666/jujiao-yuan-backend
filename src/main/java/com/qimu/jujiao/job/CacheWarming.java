package com.qimu.jujiao.job;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.gson.Gson;
import com.qimu.jujiao.model.entity.Team;
import com.qimu.jujiao.model.entity.User;
import com.qimu.jujiao.model.vo.TeamUserVo;
import com.qimu.jujiao.service.TeamService;
import com.qimu.jujiao.service.UserService;
import com.qimu.jujiao.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


/**
 * @Author: QiMu
 * @Date: 2023年03月12日 13:40
 * @Version: 1.0
 * @Description: 自动任务
 */
@Component
@Slf4j
public class CacheWarming {
    @Resource
    private UserService userService;

    @Resource
    private TeamService teamService;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private RedissonClient redissonClient;

    private List<Long> mainUserList = Arrays.asList(1L);

    /**
     * 每天的2点半更新缓存
     */
    @Scheduled(cron = "0 30 2 * * *")
    public void searchUserList() {
        RLock rLock = redissonClient.getLock("jujiaoyuan:cache:searchUserList:lock");
        try {
            if (rLock.tryLock(0, -1, TimeUnit.MILLISECONDS)) {
                for (Long mainUserId : mainUserList) {
                    QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
                    List<User> list = userService.list(userQueryWrapper);
                    List<User> result = list.stream().map(user -> userService.getSafetyUser(user)).collect(Collectors.toList());
                    try {
                        redisTemplate.opsForValue().set(userService.redisFormat(mainUserId), result, 1 + RandomUtil.randomInt(1, 5), TimeUnit.MINUTES);
                    } catch (Exception e) {
                        log.error("redis set key error", e);
                    }
                }
            }
        } catch (InterruptedException e) {
            log.error("CacheWarming searchUserList error ", e);
        } finally {
            if (rLock.isHeldByCurrentThread()) {
                System.out.println("unLock: " + Thread.currentThread().getId());
                rLock.unlock();
            }
        }
    }

    /**
     * 每天凌晨2:20缓存所有队伍
     */
    @Scheduled(cron = "0 20 2 * * *")
    public void searchTeamList() {
        RLock rLock = redissonClient.getLock("jujiaoyuan:cache:searchTeamList:lock");
        try {
            if (rLock.tryLock(0, -1, TimeUnit.MILLISECONDS)) {
                List<Team> list = teamService.list();
                TeamUserVo teamUserVo = teamService.teamSet(list);
                try {
                    redisTemplate.opsForValue().set("jujiaoyuan:team:getTeams:getTeams", teamUserVo, 1 + RandomUtil.randomInt(1, 5), TimeUnit.MINUTES);
                } catch (Exception e) {
                    log.error("redis set key error", e);
                }
            }
        } catch (InterruptedException e) {
            log.error("CacheWarming searchTeamList error ", e);
        } finally {
            if (rLock.isHeldByCurrentThread()) {
                System.out.println("unLock: " + Thread.currentThread().getId());
                rLock.unlock();
            }
        }
    }

    /**
     * 每天凌晨0点解散过期的队伍
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional(rollbackFor = Exception.class)
    public void dissolutionExpiredTeam() {
        RLock rLock = redissonClient.getLock("jujiaoyuan:cache:dissolutionExpiredTeam:lock");
        try {
            if (rLock.tryLock(0, -1, TimeUnit.MILLISECONDS)) {
                List<Team> teamList = teamService.list();
                Set<Long> teams = teamList.stream()
                        .filter(team -> new Date().after(team.getExpireTime()))
                        .map(Team::getId).collect(Collectors.toSet());
                // 如果没有过期的队伍就不再执行后续操作
                if (teams.isEmpty() || teamList.size() <= 0) {
                    return;
                }
                List<User> userList = userService.list();
                userList.forEach(user -> {
                    Set<Long> userTeamIds = StringUtils.stringJsonListToLongSet(user.getTeamIds());
                    for (Long teamId : teams) {
                        userTeamIds.remove(teamId);
                    }
                    user.setTeamIds(new Gson().toJson(userTeamIds));
                    userService.updateById(user);
                });
                teamService.removeByIds(teams);
            }
        } catch (InterruptedException e) {
            log.error("CacheWarming dissolutionExpiredTeam error ", e);
        } finally {
            if (rLock.isHeldByCurrentThread()) {
                System.out.println("unLock: " + Thread.currentThread().getId());
                rLock.unlock();
            }
        }
    }
}
