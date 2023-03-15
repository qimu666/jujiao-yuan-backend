package com.qimu.jujiao.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Set;

/**
 * @Author: QiMu
 * @Date: 2023年03月10日 21:45
 * @Version: 1.0
 * @Description: 队伍和用户信息封装类（脱敏）
 */
@Data
public class TeamUserVo implements Serializable {
    private static final long serialVersionUID = 4408963399165943029L;

    private Set<TeamVo> teamSet;
}
