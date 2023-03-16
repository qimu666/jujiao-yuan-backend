package com.qimu.jujiao.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.qimu.jujiao.model.entity.Team;
import com.qimu.jujiao.model.entity.User;
import com.qimu.jujiao.model.request.TeamJoinRequest;
import com.qimu.jujiao.model.vo.TeamUserVo;
import com.qimu.jujiao.model.vo.TeamVo;

import javax.servlet.http.HttpServletRequest;
import java.util.Set;

/**
 * @author qimu
 * @description 针对表【team(队伍)】的数据库操作Service
 * @createDate 2023-03-08 23:14:16
 */
public interface TeamService extends IService<Team> {

    /**
     * 获取所有队伍
     *
     * @return
     */
    TeamUserVo getTeams();

    /**
     * 根据队伍id获取队伍
     *
     * @param teamId
     * @param request
     * @return
     */
    TeamVo getUsersByTeamId(Long teamId, HttpServletRequest request);

    /**
     * 根据用户加入的队伍id获取队伍信息
     *
     * @param teamId
     * @param request
     * @return
     */
    TeamUserVo getTeamListByTeamIds(Set<Long> teamId, HttpServletRequest request);

    /**
     * 加入队伍
     *
     * @param joinTeam
     * @param loginUser
     * @return
     */
    User joinTeam(TeamJoinRequest joinTeam, User loginUser);
}
