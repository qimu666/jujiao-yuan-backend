package com.qimu.jujiao.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author: QiMu
 * @Date: 2023年03月10日 17:22
 * @Version: 1.0
 * @Description:
 */
@Data
public class UserUpdateRequest implements Serializable {
    private static final long serialVersionUID = 2208508457574775689L;
    Integer id;
    String field;
    String editValue;
}
