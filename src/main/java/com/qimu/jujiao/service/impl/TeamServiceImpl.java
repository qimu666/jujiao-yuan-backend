package com.qimu.jujiao.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.qimu.jujiao.common.ErrorCode;
import com.qimu.jujiao.exception.BusinessException;
import com.qimu.jujiao.mapper.TeamMapper;
import com.qimu.jujiao.model.entity.Team;
import com.qimu.jujiao.model.entity.User;
import com.qimu.jujiao.model.request.TeamJoinRequest;
import com.qimu.jujiao.model.vo.TeamUserVo;
import com.qimu.jujiao.model.vo.TeamVo;
import com.qimu.jujiao.service.TeamService;
import com.qimu.jujiao.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author qimu
 * @description 针对表【team(队伍)】的数据库操作Service实现
 * @createDate 2023-03-08 23:14:16
 */
@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team> implements TeamService {

    @Resource
    private UserService userService;

    @Override
    public TeamUserVo getTeamListByTeamIds(Set<Long> teamId, HttpServletRequest request) {
        userService.isLogin(request);
        if (CollectionUtils.isEmpty(teamId)) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "信息有误");
        }
        // 获取所有队伍
        List<Team> teams = this.list();
        // 过滤后的队伍列表
        List<Team> teamList = teams.stream().filter(team -> {
            for (Long tid : teamId) {
                if (tid.equals(team.getId())) {
                    return true;
                }
            }
            return false;
        }).collect(Collectors.toList());
        TeamUserVo teamUserVo = new TeamUserVo();
        teamSet(teamList, teamUserVo);
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
        if (team.getTeamStatus() == 3) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "当前队伍私有,不可加入");
        }
        // 当前队伍是不是加密队伍
        if (team.getTeamStatus() == 2) {
            if (StringUtils.isBlank(joinTeam.getPassword()) || !joinTeam.getPassword().equals(team.getTeamPassword())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
            }
        }
        Gson gson = new Gson();
        // 当前队伍加入的队员id
        String usersId = team.getUsersId();
        Set<Long> userIdList = gson.fromJson(usersId, new TypeToken<Set<Long>>() {
        }.getType());
        userIdList = Optional.ofNullable(userIdList).orElse(new HashSet<>());
        // 当前队伍是不是已经满人了
        if (userIdList.size() >= team.getMaxNum()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "当前队伍人数已满");
        }
        // 当前用户已经加入的队伍
        User user = userService.getById(loginUser);
        String teamIds = user.getTeamIds();
        Set<Long> loginUserTeamIdList = gson.fromJson(teamIds, new TypeToken<Set<Long>>() {
        }.getType());
        loginUserTeamIdList = Optional.ofNullable(loginUserTeamIdList).orElse(new HashSet<>());

        // 最多加入5个队伍
        if (loginUserTeamIdList.size() > 5) {
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

        boolean joinTeamStatus = this.updateById(team);
        boolean loginJoinTeamStatus = userService.updateById(user);

        if (!(joinTeamStatus && loginJoinTeamStatus)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "加入失败");
        }
        return userService.getSafetyUser(user);
    }

    @Override
    public TeamUserVo getTeams() {
        List<Team> teams = this.list();
        TeamUserVo teamUserVo = new TeamUserVo();
        teamSet(teams, teamUserVo);
        return teamUserVo;
    }

    @Override
    public TeamVo getUsersByTeamId(Long teamId, HttpServletRequest request) {
        // 当前用户是否登录
        User loginUser = userService.getLoginUser(request);
        User user = userService.getById(loginUser.getId());
        // 当前用户加入的队伍id
        String userTeamIds = user.getTeamIds();
        Gson gson = new Gson();
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
        Set<Long> userTeamIdSet = gson.fromJson(userTeamIds, new TypeToken<Set<Long>>() {
        }.getType());
        userTeamIdSet = Optional.ofNullable(userTeamIdSet).orElse(new HashSet<>());

        Set<Long> usersIdSet = gson.fromJson(usersId, new TypeToken<Set<Long>>() {
        }.getType());
        usersIdSet = Optional.ofNullable(usersIdSet).orElse(new HashSet<>());

        // 当前用户不是管理员
        // 当前用户加入的队伍的ids中不包含传过来的队伍id
        // 当前用户的id不等于队伍的创建者id 说明没权限
        boolean noPermissions = !userService.isAdmin(loginUser) && !userTeamIdSet.contains(teamId) && loginUser.getId() != userId;
        if (noPermissions) {
            throw new BusinessException(ErrorCode.NO_AUTH, "暂无权限查看");
        }

        Set<User> users = new HashSet<>();
        for (Long id : usersIdSet) {
            users.add(userService.getById(id));
        }
        users = users.stream().map(userService::getSafetyUser).collect(Collectors.toSet());
        User createTeamUser = userService.getById(userId);
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
        teamVo.setUser(createTeamUser);
        teamVo.setUserSet(users);
        return teamVo;
    }


    /**
     * 处理返回信息Vo
     *
     * @param teamList
     * @param teamUserVo
     */
    private void teamSet(List<Team> teamList, TeamUserVo teamUserVo) {
        Gson gson = new Gson();
        Set<TeamVo> users = new HashSet<>();
        teamList.forEach(team -> {
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
            Set<Long> userSet = gson.fromJson(usersId, new TypeToken<Set<Long>>() {
            }.getType());
            userSet = Optional.ofNullable(userSet).orElse(new HashSet<>());
            Set<User> userList = new HashSet<>();
            for (Long id : userSet) {
                userList.add(userService.getById(id));
            }
            User createUser = userService.getById(team.getUserId());
            teamVo.setUser(createUser);
            userList = userList.stream().map(userService::getSafetyUser).collect(Collectors.toSet());
            teamVo.setUserSet(userList);
            users.add(teamVo);
        });
        teamUserVo.setTeamSet(users);
    }
}




