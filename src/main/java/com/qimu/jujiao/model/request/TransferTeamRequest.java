package com.qimu.jujiao.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author: QiMu
 * @Date: 2023年03月25日 16:35
 * @Version: 1.0
 * @Description:
 */
@Data
public class TransferTeamRequest implements Serializable {
    private static final long serialVersionUID = -6119912852151581286L;
    private String userAccount;
    private Long teamId;
}
