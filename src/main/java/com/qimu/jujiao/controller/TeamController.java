package com.qimu.jujiao.controller;

import com.qimu.jujiao.common.BaseResponse;
import com.qimu.jujiao.common.ErrorCode;
import com.qimu.jujiao.common.ResultUtil;
import com.qimu.jujiao.exception.BusinessException;
import com.qimu.jujiao.model.vo.TeamUserVo;
import com.qimu.jujiao.service.TeamService;
import com.qimu.jujiao.service.UserService;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

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

    @GetMapping("/teamsById")
    public BaseResponse<TeamUserVo> getTeamsByIds(@RequestParam(required = false) List<Long> teamId) {
        if (CollectionUtils.isEmpty(teamId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该用户暂未加入队伍");
        }
        TeamUserVo teams = teamService.getTeamsList(teamId);
        return ResultUtil.success(teams);
    }


    @GetMapping("/teams")
    public BaseResponse<TeamUserVo> getTeams() {
        TeamUserVo teams = teamService.getTeams();
        return ResultUtil.success(teams);
    }
}
