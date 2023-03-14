package com.qimu.jujiao.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.qimu.jujiao.mapper.TeamMapper;
import com.qimu.jujiao.model.entity.Team;
import com.qimu.jujiao.model.entity.User;
import com.qimu.jujiao.model.vo.TeamUserVo;
import com.qimu.jujiao.model.vo.TeamVo;
import com.qimu.jujiao.service.TeamService;
import com.qimu.jujiao.service.UserService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author qimu
 * @description 针对表【team(队伍)】的数据库操作Service实现
 * @createDate 2023-03-08 23:14:16
 */
@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team>
        implements TeamService {
    @Resource
    private TeamMapper teamMapper;
    @Resource
    private UserService userService;

    @Override
    public TeamUserVo getTeamsList(List<Long> teamId) {
        List<Team> teams = this.list();
        TeamUserVo teamUserVo = new TeamUserVo();
        List<Team> teamList = teams.stream().filter(team -> {
            for (Long tid : teamId) {
                if (tid.equals(team.getId())) {
                    return true;
                }
            }
            return false;
        }).collect(Collectors.toList());

        teamList(teamList, teamUserVo);
        return teamUserVo;
    }

    @Override
    public TeamUserVo getTeams() {
        List<Team> teams = this.list();
        TeamUserVo teamUserVo = new TeamUserVo();
        teamList(teams, teamUserVo);
        return teamUserVo;
    }

    /**
     * 处理返回信息Vo
     *
     * @param teams
     * @param teamUserVo
     */
    private void teamList(List<Team> teams, TeamUserVo teamUserVo) {
        List<User> usersAll = userService.list();
        Gson gson = new Gson();
        List<User> userList = usersAll.stream().filter(user -> {
            String teamIds = user.getTeamIds();
            Set<Long> teamSerIds = gson.fromJson(teamIds, new TypeToken<Set<Long>>() {
            }.getType());
            teamSerIds = Optional.ofNullable(teamSerIds).orElse(new HashSet<>());
            for (Team team : teams) {
                if (teamSerIds.contains(team.getId())) {
                    return true;
                }
            }
            return false;
        }).map(userService::getSafetyUser).collect(Collectors.toList());

        Set<TeamVo> teamVos = new HashSet<>();
        for (Team team : teams) {
            Long userId = team.getUserId();
            TeamVo teamVo = new TeamVo();
            Long id = team.getId();
            teamVo.setId(team.getId());
            teamVo.setTeamName(team.getTeamName());
            teamVo.setTeamAvatarUrl(team.getTeamAvatarUrl());
            teamVo.setTeamPassword(team.getTeamPassword());
            teamVo.setTeamDesc(team.getTeamDesc());
            teamVo.setMaxNum(team.getMaxNum());
            teamVo.setExpireTime(team.getExpireTime());
            teamVo.setTeamStatus(team.getTeamStatus());
            teamVo.setCreateTime(team.getCreateTime());
            teamVo.setAnnounce(team.getAnnounce());
            User createTeamUser = userService.getById(userId);
            teamVo.setUser(createTeamUser);
            List<User> users = new ArrayList<>();
            for (User user : userList) {
                String teamIds = user.getTeamIds();
                Set<Long> teamSerIds = gson.fromJson(teamIds, new TypeToken<Set<Long>>() {
                }.getType());
                teamSerIds = Optional.ofNullable(teamSerIds).orElse(new HashSet<>());
                if (teamSerIds.contains(id)) {
                    users.add(user);
                }
            }
            teamVo.setUserList(users);
            teamVos.add(teamVo);
        }
        teamUserVo.setTeamList(teamVos);
    }
}




