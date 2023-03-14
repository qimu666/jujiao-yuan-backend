package com.qimu.jujiao.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.qimu.jujiao.model.entity.Team;
import com.qimu.jujiao.model.vo.TeamUserVo;

import java.util.List;

/**
* @author qimu
* @description 针对表【team(队伍)】的数据库操作Service
* @createDate 2023-03-08 23:14:16
*/
public interface TeamService extends IService<Team> {

    TeamUserVo getTeamsList(List<Long> teamId);

    TeamUserVo getTeams();
}
