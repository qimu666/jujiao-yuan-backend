package com.qimu.jujiao.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author: QiMu
 * @Date: 2023年03月24日 16:46
 * @Version: 1.0
 * @Description:
 */
@Data
public class KickOutUserRequest implements Serializable {
    private static final long serialVersionUID = 5733340462955698625L;
    Long teamId;
    Long userId;
}
