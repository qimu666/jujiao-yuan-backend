package com.qimu.jujiao.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author: QiMu
 * @Date: 2023年03月22日 16:43
 * @Version: 1.0
 * @Description:
 */
@Data
public class TeamQueryRequest implements Serializable {
    private static final long serialVersionUID = -1968358494521098572L;

    /**
     * 查询队伍
     */
    private String searchText;
}
