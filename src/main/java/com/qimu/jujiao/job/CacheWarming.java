package com.qimu.jujiao.job;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.gson.Gson;
import com.qimu.jujiao.model.entity.Friends;
import com.qimu.jujiao.model.entity.Team;
import com.qimu.jujiao.model.entity.User;
import com.qimu.jujiao.model.vo.TeamUserVo;
import com.qimu.jujiao.service.ChatService;
import com.qimu.jujiao.service.FriendsService;
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

import static com.qimu.jujiao.contant.FriendConstant.AGREE_STATUS;
import static com.qimu.jujiao.contant.FriendConstant.EXPIRED_STATUS;


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
    private ChatService chatService;
    @Resource
    private TeamService teamService;
    @Resource
    private FriendsService friendsService;
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
                        redisTemplate.opsForValue().set(userService.redisFormat(mainUserId), result, 1 + RandomUtil.randomInt(1, 2) / 10, TimeUnit.MINUTES);
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

    /**
     * 每7天清空一次数据,即聊天记录只保存7天
     */
    // @Scheduled(cron = "0 0 0 */7 * ?")
    public void chatRecords() {
        RLock rLock = redissonClient.getLock("jujiaoyuan:cache:chatRecords:lock");
        try {
            if (rLock.tryLock(0, -1, TimeUnit.MILLISECONDS)) {
                boolean remove = chatService.remove(null);
            }
        } catch (InterruptedException e) {
            log.error("CacheWarming chatRecords error ", e);
        } finally {
            if (rLock.isHeldByCurrentThread()) {
                System.out.println("unLock: " + Thread.currentThread().getId());
                rLock.unlock();
            }
        }
    }

    /**
     * 每30分钟校验有没有过期
     */
    @Scheduled(cron = "* 20 * * * ?")
    public void isExpires() {
        RLock rLock = redissonClient.getLock("jujiaoyuan:cache:isExpires:lock");
        try {
            if (rLock.tryLock(0, -1, TimeUnit.MILLISECONDS)) {
                List<Friends> friendsList = friendsService.list();
                friendsList.forEach(friends -> {
                    if (DateUtil.between(new Date(), friends.getCreateTime(), DateUnit.DAY) >= 3) {
                        if (friends.getStatus() != EXPIRED_STATUS && friends.getStatus() != AGREE_STATUS) {
                            friends.setStatus(2);
                            friendsService.updateById(friends);
                        }
                    }
                });
            }
        } catch (InterruptedException e) {
            log.error("CacheWarming isExpires error ", e);
        } finally {
            if (rLock.isHeldByCurrentThread()) {
                System.out.println("unLock: " + Thread.currentThread().getId());
                rLock.unlock();
            }
        }
    }
}
