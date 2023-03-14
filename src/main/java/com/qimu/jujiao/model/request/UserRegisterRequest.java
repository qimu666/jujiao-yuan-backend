package com.qimu.jujiao.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author: QiMu
 * @Date: 2023年01月16日 23:32
 * @Version: 1.0
 * @Description: 用户注册请求体
 */
@Data
public class UserRegisterRequest implements Serializable {
    private static final long serialVersionUID = -921225522179459481L;
    private String username;
    private String userAccount;
    private String userPassword;
    private String checkPassword;
}
