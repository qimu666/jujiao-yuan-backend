package com.qimu.jujiao.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author: QiMu
 * @Date: 2023年04月24日 11:24
 * @Version: 1.0
 * @Description:
 */
@Data
public class QQLoginRequest implements Serializable {
    private static final long serialVersionUID = -2110552558438970973L;
    private String code;
}
