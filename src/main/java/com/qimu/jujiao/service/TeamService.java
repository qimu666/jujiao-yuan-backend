package com.qimu.jujiao.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.qimu.jujiao.model.entity.Team;
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

    TeamUserVo getTeams();

    TeamVo getUsersByTeamId(Long teamId, HttpServletRequest request);

    TeamUserVo getTeamListByTeamIds(Set<Long> teamId, HttpServletRequest request);
}
