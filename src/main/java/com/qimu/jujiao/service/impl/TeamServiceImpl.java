package com.qimu.jujiao.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.qimu.jujiao.common.ErrorCode;
import com.qimu.jujiao.exception.BusinessException;
import com.qimu.jujiao.mapper.TeamMapper;
import com.qimu.jujiao.model.entity.Team;
import com.qimu.jujiao.model.entity.User;
import com.qimu.jujiao.model.request.*;
import com.qimu.jujiao.model.vo.TeamUserVo;
import com.qimu.jujiao.model.vo.TeamVo;
import com.qimu.jujiao.service.TeamService;
import com.qimu.jujiao.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.qimu.jujiao.contant.TeamConstant.*;
import static com.qimu.jujiao.utils.StringUtils.stringJsonListToLongSet;

/**
 * @author qimu
 * @description 针对表【team(队伍)】的数据库操作Service实现
 * @createDate 2023-03-08 23:14:16
 */
@Service
@Slf4j
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team> implements TeamService {
    private static final String SALT = "qimu_team";
    private static final String BY_TEAM_IDS = String.format("jujiaoyuan:team:getTeamListByTeamIds:%s", "byTeamIds");
    private static final String TEAMS_KEY = String.format("jujiaoyuan:team:getTeams:%s", "getTeams");
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private UserService userService;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public TeamUserVo getTeamListByTeamIds(Set<Long> teamId, HttpServletRequest request) {
        userService.isLogin(request);
        if (CollectionUtils.isEmpty(teamId)) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "信息有误");
        }
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        TeamUserVo teamsList = (TeamUserVo) valueOperations.get(BY_TEAM_IDS);
        if (teamsList != null) {
            return teamsList;
        }
        // 获取所有队伍
        List<Team> teams = this.list();
        // 过滤后的队伍列表
        List<Team> teamList = teams.stream().filter(team -> {
            for (Long tid : teamId) {
                // 保留当前没有过期的队伍和搜索的队伍
                if (!new Date().after(team.getExpireTime()) && tid.equals(team.getId())) {
                    return true;
                }
            }
            return false;
        }).collect(Collectors.toList());
        TeamUserVo teamUserVo = teamSet(teamList);
        setRedis(BY_TEAM_IDS, teamUserVo);
        return teamUserVo;
    }

    @Override
    public TeamUserVo teamQuery(TeamQueryRequest teamQueryRequest, HttpServletRequest request) {
        userService.isLogin(request);
        String searchText = teamQueryRequest.getSearchText();
        String teamQueryKey = String.format("jujiaoyuan:team:teamQuery:%s", searchText);
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        TeamUserVo teamList = (TeamUserVo) valueOperations.get(teamQueryKey);
        if (teamList != null) {
            return teamList;
        }
        LambdaQueryWrapper<Team> teamLambdaQueryWrapper = new LambdaQueryWrapper<>();
        teamLambdaQueryWrapper.like(Team::getTeamDesc, searchText.trim())
                .or().like(Team::getTeamName, searchText.trim());
        List<Team> teams = this.list(teamLambdaQueryWrapper);
        // 过滤后的队伍列表
        TeamUserVo teamUserVo = teamSet(teams);
        setRedis(teamQueryKey, teamUserVo);
        return teamUserVo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public User joinTeam(TeamJoinRequest joinTeam, User loginUser) {
        if (joinTeam == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "加入队伍有误");
        }
        Team team = this.getById(joinTeam.getTeamId());
        Date expireTime = team.getExpireTime();

        // 当前队伍有没有过期
        if (expireTime != null && expireTime.before(new Date())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "当前队伍已过期");
        }
        // 当前队伍有没有私密
        if (team.getTeamStatus() == PRIVATE_TEAM_STATUS) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "当前队伍私有,不可加入");
        }
        // 队伍密码加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + joinTeam.getPassword()).getBytes(StandardCharsets.UTF_8));
        // 当前队伍是加密队伍
        // 不是管理员需要密码
        if (!userService.isAdmin(loginUser) && team.getTeamStatus() == ENCRYPTION_TEAM_STATUS) {
            if (StringUtils.isBlank(joinTeam.getPassword()) || !encryptPassword.equals(team.getTeamPassword())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
            }
        }
        RLock lock = redissonClient.getLock("jujiaoyuan:join_team");
        try {
            // 抢到锁并执行
            while (true) {
                if (lock.tryLock(0, -1, TimeUnit.MILLISECONDS)) {
                    Gson gson = new Gson();
                    // 当前队伍加入的队员id
                    String usersId = team.getUsersId();
                    Set<Long> userIdList = stringJsonListToLongSet(usersId);
                    // 当前队伍是不是已经满人了
                    // 可以补位两个人
                    if (userIdList.size() >= team.getMaxNum() + NUMBER_OF_PLACES_TO_BE_FILLED) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "当前队伍人数已满");
                    }
                    // 当前用户已经加入的队伍
                    User user = userService.getById(loginUser);
                    String teamIds = user.getTeamIds();
                    Set<Long> loginUserTeamIdList = stringJsonListToLongSet(teamIds);

                    // 最多加入5个队伍
                    if (!userService.isAdmin(user) && loginUserTeamIdList.size() >= 5) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "最多加入5个队伍");
                    }
                    // 是否已经加入该队伍
                    if (userIdList.contains(loginUser.getId()) || loginUserTeamIdList.contains(joinTeam.getTeamId())) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "已经加入过当前队伍");
                    }
                    userIdList.add(loginUser.getId());
                    String newUserid = gson.toJson(userIdList);
                    team.setUsersId(newUserid);

                    loginUserTeamIdList.add(joinTeam.getTeamId());
                    String loginTeamsId = gson.toJson(loginUserTeamIdList);
                    user.setTeamIds(loginTeamsId);

                    boolean joinTeamStatus = this.updateById(team) && userService.updateById(user);

                    if (!joinTeamStatus) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "加入失败");
                    }
                    String teamIdKey = String.format("jujiaoyuan:team:getUsersByTeamId:%s", team.getId());
                    deleteRedisKey(teamIdKey);
                    redisTemplate.delete(userService.redisFormat(user.getId()));
                    return userService.getSafetyUser(user);
                }
            }
        } catch (InterruptedException e) {
            log.error("joinTeam error", e);
            return null;
        } finally {
            // 只能释放自己的锁
            if (lock.isHeldByCurrentThread()) {
                System.out.println("unLock: " + Thread.currentThread().getId());
                lock.unlock();
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createTeam(TeamCreateRequest teamCreateRequest, User loginUser) {
        if (StringUtils.isAnyBlank(teamCreateRequest.getTeamDesc(), teamCreateRequest.getTeamName(), teamCreateRequest.getAnnounce())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "输入不能为空");
        }
        if (teamCreateRequest.getTeamName().length() > 16) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍名称超过16个字符");
        }
        if (teamCreateRequest.getTeamDesc().length() > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍描述超过50个字符");
        }
        if (teamCreateRequest.getAnnounce().length() > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍公告超过50个字符");
        }
        // 过期时间在当前日期之前
        checkTeam(teamCreateRequest);
        int status = Optional.ofNullable(teamCreateRequest.getTeamStatus()).orElse(0);
        Team team = new Team();
        // 只有队伍状态为加密才需要设置密码
        if (status == ENCRYPTION_TEAM_STATUS) {
            if (StringUtils.isBlank(teamCreateRequest.getTeamPassword())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "加密状态,必须设置密码");
            }
            // 加密队伍校验
            encryptTeamCheck(teamCreateRequest.getTeamPassword(), team);
        }
        long id = loginUser.getId();
        User user = userService.getById(id);
        String teamIds = user.getTeamIds();
        RLock lock = redissonClient.getLock("jujiaoyuan:create_team");
        try {
            // 抢到锁并执行
            while (true) {
                if (lock.tryLock(0, -1, TimeUnit.MILLISECONDS)) {
                    Gson gson = new Gson();
                    Set<Long> teamIdList = stringJsonListToLongSet(teamIds);
                    if (!userService.isAdmin(loginUser) && teamIdList.size() >= 5) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "最多只能拥有5个队伍");
                    }
                    team.setTeamName(teamCreateRequest.getTeamName());
                    team.setTeamDesc(teamCreateRequest.getTeamDesc());
                    team.setMaxNum(teamCreateRequest.getMaxNum());
                    team.setExpireTime(teamCreateRequest.getExpireTime());
                    team.setUserId(loginUser.getId());
                    team.setUsersId("[]");
                    team.setTeamStatus(status);
                    team.setCreateTime(new Date());
                    team.setUpdateTime(new Date());
                    team.setAnnounce(teamCreateRequest.getAnnounce());
                    team.setTeamAvatarUrl(teamCreateRequest.getTeamAvatarUrl());
                    boolean createTeam = this.save(team);
                    if (!createTeam) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "创建队伍失败");
                    }
                    Team newTeam = this.getById(team);
                    String usersId = newTeam.getUsersId();
                    Set<Long> usersIdList = stringJsonListToLongSet(usersId);
                    usersIdList.add(loginUser.getId());

                    // 新队伍队员列表
                    String users = gson.toJson(usersIdList);
                    newTeam.setUsersId(users);

                    teamIdList.add(newTeam.getId());
                    // 用户新队伍json数组
                    String teams = gson.toJson(teamIdList);
                    user.setTeamIds(teams);

                    boolean createTeamStatus = userService.updateById(user) && this.updateById(newTeam);
                    if (createTeamStatus) {
                        deleteRedisKey(null);
                        redisTemplate.delete(userService.redisFormat(user.getId()));
                    }
                    return createTeamStatus;
                }
            }
        } catch (InterruptedException e) {
            log.error("joinTeam error", e);
            return false;
        } finally {
            // 只能释放自己的锁
            if (lock.isHeldByCurrentThread()) {
                System.out.println("unLock: " + Thread.currentThread().getId());
                lock.unlock();
            }
        }
    }

    /**
     * 加密队伍校验
     *
     * @param teamCreateRequest
     * @param team
     */
    private void encryptTeamCheck(String teamCreateRequest, Team team) {
        if (teamCreateRequest.length() < 6) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍密码长度低于6位");
        }
        if (teamCreateRequest.length() > 16) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码最长只能设置16位");
        }
        // 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + teamCreateRequest).getBytes(StandardCharsets.UTF_8));
        team.setTeamPassword(encryptPassword);
    }

    /**
     * 校验队伍
     *
     * @param teamCreateRequest
     */
    private void checkTeam(TeamCreateRequest teamCreateRequest) {
        if (new Date().after(teamCreateRequest.getExpireTime())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "过期时间不能在当前时间之前");
        }
        if (teamCreateRequest.getMaxNum() == null || teamCreateRequest.getMaxNum() > 10) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍最多只能容纳10人");
        }
        if (teamCreateRequest.getMaxNum() < 5) {
            teamCreateRequest.setMaxNum(5);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍最少要有5人");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateTeam(TeamUpdateRequest teamUpdateRequest, User loginUser) {
        Long id = teamUpdateRequest.getId();
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "当前队伍不存在");
        }
        if (StringUtils.isAnyBlank(teamUpdateRequest.getTeamName(), teamUpdateRequest.getAnnounce(), teamUpdateRequest.getTeamDesc())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "输入不能为空");
        }
        if (teamUpdateRequest.getTeamName().length() > 16) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍名称不能超过16个字符");
        }
        if (teamUpdateRequest.getTeamDesc().length() > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍描述不能超过50个字符");
        }
        if (teamUpdateRequest.getAnnounce().length() > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍公告不能超过50个字符");
        }
        if (teamUpdateRequest.getExpireTime() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "过期时间不能为空");
        }
        // 过期时间在当前日期之前
        if (new Date().after(teamUpdateRequest.getExpireTime())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "过期时间不能在当前时间之前");
        }
        if (teamUpdateRequest.getMaxNum() == null || teamUpdateRequest.getMaxNum() > 10) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍最多只能容纳10人");
        }
        if (teamUpdateRequest.getMaxNum() < 5) {
            teamUpdateRequest.setMaxNum(5);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍最少要有5人");
        }
        Team oldTeam = this.getById(id);
        if (oldTeam == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "队伍不存在");
        }
        // 只有管理员或者队伍的创建者可以修改
        if (oldTeam.getUserId() != loginUser.getId() && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "暂无权限");
        }
        int status = Optional.ofNullable(teamUpdateRequest.getTeamStatus()).orElse(0);
        if (status == ENCRYPTION_TEAM_STATUS) {
            if (!StringUtils.isBlank(teamUpdateRequest.getTeamPassword())) {
                // 加密队伍校验
                encryptTeamCheck(teamUpdateRequest.getTeamPassword(), oldTeam);
            }
            if (StringUtils.isBlank(oldTeam.getTeamPassword())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "加密状态,必须设置密码");
            }
        }
        oldTeam.setTeamName(teamUpdateRequest.getTeamName());
        oldTeam.setTeamAvatarUrl(teamUpdateRequest.getTeamAvatarUrl());
        oldTeam.setTeamDesc(teamUpdateRequest.getTeamDesc());
        oldTeam.setMaxNum(teamUpdateRequest.getMaxNum());
        oldTeam.setExpireTime(teamUpdateRequest.getExpireTime());
        oldTeam.setTeamStatus(teamUpdateRequest.getTeamStatus());
        oldTeam.setUpdateTime(new Date());
        oldTeam.setAnnounce(teamUpdateRequest.getAnnounce());
        boolean updateStatus = this.updateById(oldTeam);
        if (updateStatus) {
            String teamIdKey = String.format("jujiaoyuan:team:getUsersByTeamId:%s", oldTeam.getId());
            deleteRedisKey(teamIdKey);
        }
        return updateStatus;
    }

    @Override
    public Boolean kickOutTeamByUserId(KickOutUserRequest kickOutUserRequest, User loginUser) {
        Long teamId = kickOutUserRequest.getTeamId();
        Long userId = kickOutUserRequest.getUserId();
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该队伍不存在");
        }
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该队员不存在");
        }
        Team team = this.getById(teamId);
        // 当前用户不是管理员、踢出的用户是队伍的创建者、当前用户不是队伍的创建者无法踢出队员
        if (!userService.isAdmin(loginUser) || team.getUserId().equals(userId) || loginUser.getId() != team.getUserId()) {
            throw new BusinessException(ErrorCode.KICK_OUT_USER, "权限不足");
        }
        User user = userService.getById(userId);
        // 当前队伍中的用户id列表
        Set<Long> userIds = stringJsonListToLongSet(team.getUsersId());
        // 踢出的用户当前的队伍id列表
        Set<Long> teamIds = stringJsonListToLongSet(user.getTeamIds());
        // 用户和队伍都删除各自的id
        userIds.remove(userId);
        teamIds.remove(teamId);
        Gson gson = new Gson();
        user.setTeamIds(gson.toJson(teamIds));
        team.setUsersId(gson.toJson(userIds));
        boolean kickOutTeamStatUs = userService.updateById(user) && this.updateById(team);
        if (kickOutTeamStatUs) {
            String teamIdKey = String.format("jujiaoyuan:team:getUsersByTeamId:%s", team.getId());
            redisTemplate.delete(userService.redisFormat(user.getId()));
            deleteRedisKey(teamIdKey);
        }
        return kickOutTeamStatUs;
    }

    @Override
    public Boolean transferTeam(TransferTeamRequest transferTeamRequest, User loginUser) {
        if (transferTeamRequest.getTeamId() == null || transferTeamRequest.getTeamId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该队伍不存在");
        }
        if (StringUtils.isBlank(transferTeamRequest.getUserAccount())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号不能为空");
        }
        Team team = this.getById(transferTeamRequest.getTeamId());
        LambdaQueryWrapper<User> userLambdaQueryWrapper = new LambdaQueryWrapper<>();
        userLambdaQueryWrapper.eq(User::getUserAccount, transferTeamRequest.getUserAccount());
        User user = userService.getOne(userLambdaQueryWrapper);
        if (user == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "该用户不存在");
        }
        Set<Long> userIds = stringJsonListToLongSet(team.getUsersId());
        Set<Long> teamIds = stringJsonListToLongSet(user.getTeamIds());

        // 新队长不在队伍中的、
        if (!userIds.contains(user.getId()) || !teamIds.contains(transferTeamRequest.getTeamId())) {
            throw new BusinessException(ErrorCode.KICK_OUT_USER, "输入用户不在队伍中");
        }
        // 当前用户不是管理员、当前用户也不是队伍的创建者无法转移
        if (!userService.isAdmin(loginUser) && loginUser.getId() != team.getUserId()) {
            throw new BusinessException(ErrorCode.KICK_OUT_USER, "权限不足");
        }
        // 队伍的创建者修改为新用户
        team.setUserId(user.getId());
        boolean update = this.updateById(team);
        if (update) {
            String teamIdKey = String.format("jujiaoyuan:team:getUsersByTeamId:%s", team.getId());
            redisTemplate.delete(userService.redisFormat(user.getId()));
            redisTemplate.delete(userService.redisFormat(loginUser.getId()));
            deleteRedisKey(teamIdKey);
        }
        return update;
    }

    /**
     * 删除redis缓存
     *
     * @param redisKey
     */
    private void deleteRedisKey(String redisKey) {
        redisTemplate.delete(TEAMS_KEY);
        redisTemplate.delete(BY_TEAM_IDS);
        if (StringUtils.isNotBlank(redisKey)) {
            redisTemplate.delete(redisKey);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean dissolutionTeam(Long teamId, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Team team = this.getById(teamId);
        if (!userService.isAdmin(loginUser) && loginUser.getId() != team.getUserId()) {
            throw new BusinessException(ErrorCode.NOT_LOGIN, "暂无权限");
        }
        List<User> users = userService.list();
        users.forEach(user -> {
            Set<Long> teamIds = stringJsonListToLongSet(user.getTeamIds());
            teamIds.remove(team.getId());
            user.setTeamIds(new Gson().toJson(teamIds));
            userService.updateById(user);
        });
        boolean dissolutionTeam = this.removeById(team);
        if (dissolutionTeam) {
            String teamIdKey = String.format("jujiaoyuan:team:getUsersByTeamId:%s", teamId);
            deleteRedisKey(teamIdKey);
            redisTemplate.delete(userService.redisFormat(loginUser.getId()));
        }
        return dissolutionTeam;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean quitTeam(Long teamId, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Team team = this.getById(teamId);
        User user = userService.getById(loginUser);
        Set<Long> teamIds = stringJsonListToLongSet(user.getTeamIds());
        teamIds.remove(team.getId());
        Set<Long> userIds = stringJsonListToLongSet(team.getUsersId());
        userIds.remove(user.getId());
        Gson gson = new Gson();
        user.setTeamIds(gson.toJson(teamIds));
        team.setUsersId(gson.toJson(userIds));
        boolean quitUserStatus = userService.updateById(user) && this.updateById(team);
        if (quitUserStatus) {
            String teamIdKey = String.format("jujiaoyuan:team:getUsersByTeamId:%s", teamId);
            deleteRedisKey(teamIdKey);
            redisTemplate.delete(userService.redisFormat(user.getId()));
        }
        return quitUserStatus;
    }

    @Override
    public TeamUserVo getTeams() {
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        TeamUserVo teamList = (TeamUserVo) valueOperations.get(TEAMS_KEY);
        if (teamList != null) {
            List<TeamVo> lists = new ArrayList<>(teamList.getTeamSet());
            Collections.shuffle(lists);
            HashSet<TeamVo> teamVos = new HashSet<>(lists);
            teamList.setTeamSet(teamVos);
            return teamList;
        }
        List<Team> teams = this.list();
        TeamUserVo teamUserVo = teamSet(teams);
        setRedis(TEAMS_KEY, teamUserVo);
        return teamUserVo;
    }

    @Override
    public TeamVo getUsersByTeamId(Long teamId, HttpServletRequest request) {
        // 当前用户是否登录
        User loginUser = userService.getLoginUser(request);
        User user = userService.getById(loginUser.getId());
        // 当前用户加入的队伍id
        String userTeamIds = user.getTeamIds();
        Team team = this.getById(teamId);

        String usersId = team.getUsersId();
        // 创建队伍者id
        Long userId = team.getUserId();

        Long tid = team.getId();
        String teamName = team.getTeamName();
        String teamAvatarUrl = team.getTeamAvatarUrl();
        String teamDesc = team.getTeamDesc();
        Integer maxNum = team.getMaxNum();
        Date expireTime = team.getExpireTime();
        Integer teamStatus = team.getTeamStatus();
        Date createTime = team.getCreateTime();
        String announce = team.getAnnounce();
        // 当前用户加入的队伍的id
        Set<Long> userTeamIdSet = stringJsonListToLongSet(userTeamIds);
        // 当前队伍加入的用户的id
        Set<Long> usersIdSet = stringJsonListToLongSet(usersId);

        // 当前用户不是管理员
        // 当前用户加入的队伍的ids中不包含传过来的队伍id
        // 当前用户的id不等于队伍的创建者id 说明没权限
        boolean noPermissions = !userService.isAdmin(loginUser) && !userTeamIdSet.contains(teamId) && loginUser.getId() != userId;
        if (noPermissions) {
            throw new BusinessException(ErrorCode.NO_AUTH, "暂无权限查看");
        }
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        String teamIdKey = String.format("jujiaoyuan:team:getUsersByTeamId:%s", teamId);
        TeamVo teams = (TeamVo) valueOperations.get(teamIdKey);
        if (teams != null) {
            return teams;
        }

        Set<User> users = new HashSet<>();
        for (Long id : usersIdSet) {
            users.add(userService.getById(id));
        }
        users = users.stream().map(userService::getSafetyUser).collect(Collectors.toSet());

        User createTeamUser = userService.getById(userId);
        User safetyUser = userService.getSafetyUser(createTeamUser);
        TeamVo teamVo = new TeamVo();
        teamVo.setId(tid);
        teamVo.setTeamName(teamName);
        teamVo.setTeamAvatarUrl(teamAvatarUrl);
        teamVo.setTeamDesc(teamDesc);
        teamVo.setMaxNum(maxNum);
        teamVo.setExpireTime(expireTime);
        teamVo.setTeamStatus(teamStatus);
        teamVo.setCreateTime(createTime);
        teamVo.setAnnounce(announce);
        teamVo.setUser(safetyUser);
        teamVo.setUserSet(users);
        setRedis(teamIdKey, teamVo);
        return teamVo;
    }

    /**
     * 设置 redis 3分钟
     *
     * @param redisKey
     * @param data
     */
    private void setRedis(String redisKey, Object data) {
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        try {
            // 解决缓存雪崩
            int i = RandomUtil.randomInt(1, 2);
            valueOperations.set(redisKey, data, 1 + i / 10, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("redis set key error");
        }
    }

    /**
     * 处理返回信息Vo
     *
     * @param teamList
     * @return teamUserVo
     */
    @Override
    public TeamUserVo teamSet(List<Team> teamList) {
        // 过滤过期的队伍
        List<Team> listTeam = teamList.stream()
                .filter(team -> !new Date().after(team.getExpireTime()))
                .collect(Collectors.toList());
        Collections.shuffle(listTeam);
        TeamUserVo teamUserVo = new TeamUserVo();
        Set<TeamVo> users = new HashSet<>();
        listTeam.forEach(team -> {
            TeamVo teamVo = new TeamVo();
            String usersId = team.getUsersId();
            teamVo.setId(team.getId());
            teamVo.setTeamName(team.getTeamName());
            teamVo.setTeamAvatarUrl(team.getTeamAvatarUrl());
            teamVo.setTeamDesc(team.getTeamDesc());
            teamVo.setMaxNum(team.getMaxNum());
            teamVo.setExpireTime(team.getExpireTime());
            teamVo.setTeamStatus(team.getTeamStatus());
            teamVo.setCreateTime(team.getCreateTime());
            teamVo.setAnnounce(team.getAnnounce());
            Set<Long> userSet = stringJsonListToLongSet(usersId);
            Set<User> userList = new HashSet<>();
            for (Long id : userSet) {
                userList.add(userService.getById(id));
            }
            User createUser = userService.getById(team.getUserId());
            User safetyUser = userService.getSafetyUser(createUser);
            teamVo.setUser(safetyUser);
            userList = userList.stream().map(userService::getSafetyUser).collect(Collectors.toSet());
            teamVo.setUserSet(userList);
            users.add(teamVo);
        });
        teamUserVo.setTeamSet(users);
        return teamUserVo;
    }
}




