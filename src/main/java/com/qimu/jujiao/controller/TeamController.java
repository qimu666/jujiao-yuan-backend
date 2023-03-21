package com.qimu.jujiao.controller;

import com.qimu.jujiao.common.BaseResponse;
import com.qimu.jujiao.common.ErrorCode;
import com.qimu.jujiao.common.ResultUtil;
import com.qimu.jujiao.exception.BusinessException;
import com.qimu.jujiao.model.entity.Team;
import com.qimu.jujiao.model.entity.User;
import com.qimu.jujiao.model.request.TeamCreateRequest;
import com.qimu.jujiao.model.request.TeamJoinRequest;
import com.qimu.jujiao.model.vo.TeamUserVo;
import com.qimu.jujiao.model.vo.TeamVo;
import com.qimu.jujiao.service.TeamService;
import com.qimu.jujiao.service.UserService;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Set;

/**
 * @Author: QiMu
 * @Date: 2023年03月10日 20:59
 * @Version: 1.0
 * @Description:
 */
@RestController
@RequestMapping("/team")
public class TeamController {
    @Resource
    private TeamService teamService;

    @Resource
    private UserService userService;

    @GetMapping("/{teamId}")
    public BaseResponse<TeamVo> getUsersByTeamId(@PathVariable("teamId") Long teamId, HttpServletRequest request) {
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该用户暂未加入队伍");
        }
        TeamVo teams = teamService.getUsersByTeamId(teamId, request);
        return ResultUtil.success(teams);
    }

    @GetMapping("/teams")
    public BaseResponse<TeamUserVo> getTeams() {
        TeamUserVo teams = teamService.getTeams();
        return ResultUtil.success(teams);
    }

    @PostMapping("/{teamId}")
    public BaseResponse<Boolean> dissolutionByTeamId(@PathVariable("teamId") Long teamId, HttpServletRequest request) {
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该用户暂未加入队伍");
        }
        boolean dissolutionTeam = teamService.dissolutionTeam(teamId,request);
        return ResultUtil.success(dissolutionTeam);
    }

    @PostMapping("/quit/{teamId}")
    public BaseResponse<Boolean> quitTeam(@PathVariable("teamId") Long teamId, HttpServletRequest request) {
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该用户暂未加入队伍");
        }
        boolean quitTeam = teamService.quitTeam(teamId,request);
        return ResultUtil.success(quitTeam);
    }
    @GetMapping("/teamsByIds")
    public BaseResponse<TeamUserVo> getTeamListByTeamIds(@RequestParam(required = false) Set<Long> teamId, HttpServletRequest request) {
        if (CollectionUtils.isEmpty(teamId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该用户暂未加入队伍");
        }
        TeamUserVo teams = teamService.getTeamListByTeamIds(teamId, request);
        return ResultUtil.success(teams);
    }

    @PostMapping("/join")
    public BaseResponse<User> joinTeam(@RequestBody TeamJoinRequest joinTeam, HttpServletRequest request) {
        if (joinTeam == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "加入队伍失败");
        }
        User loginUser = userService.getLoginUser(request);
        User joinUser = teamService.joinTeam(joinTeam, loginUser);
        return ResultUtil.success(joinUser);
    }

    @PostMapping("/createTeam")
    public BaseResponse<Boolean> createTeam(@RequestBody TeamCreateRequest teamCreateRequest, HttpServletRequest request) {
        if (teamCreateRequest == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "创建队伍失败");
        }
        User loginUser = userService.getLoginUser(request);
        Boolean team = teamService.createTeam(teamCreateRequest, loginUser);
        return ResultUtil.success(team);
    }
}
