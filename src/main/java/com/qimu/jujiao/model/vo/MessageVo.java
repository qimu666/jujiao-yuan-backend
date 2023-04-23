package com.qimu.jujiao.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author: QiMu
 * @Date: 2023年04月10日 14:10
 * @Version: 1.0
 * @Description:
 */
@Data
public class MessageVo implements Serializable {
    private static final long serialVersionUID = -4722378360550337925L;
    private WebSocketVo formUser;
    private WebSocketVo toUser;
    private Long teamId;
    private String text;
    private Boolean isMy = false;
    private Integer chatType;
    private Boolean isAdmin = false;
    private String createTime;
}
