package com.qimu.jujiao.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author: QiMu
 * @Date: 2023年04月17日 09:34
 * @Version: 1.0
 * @Description:
 */
@Data
public class FriendAddRequest implements Serializable {
    private static final long serialVersionUID = 1472823745422792988L;

    private Long id;
    /**
     * 接收申请的用户id
     */
    private Long receiveId;

    /**
     * 好友申请备注信息
     */
    private String remark;
}
