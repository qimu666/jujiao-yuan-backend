package com.qimu.jujiao.contant;

/**
 * @author: QiMu
 * @Date: 2023年01月18日 21:54
 * @Version: 1.0
 * @Description: 用户常量
 */
public interface TeamConstant {
    /**
     * 队伍共开状态(默认)
     */
    int PUBLIC_TEAM_STATUS = 0;
    /**
     * 队伍私有状态
     */
    int PRIVATE_TEAM_STATUS = 1;
    /**
     * 队伍加密状态
     */
    int ENCRYPTION_TEAM_STATUS = 2;

    /**
     * 候补人数
     */
    int NUMBER_OF_PLACES_TO_BE_FILLED = 2;
}
