package com.qimu.jujiao.common;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author: QiMu
 * @Date: 2023年02月04日 19:38
 * @Version: 1.0
 * @Description: 通用返回类
 */
@Data
public class BaseResponse<T> implements Serializable {
    private static final long serialVersionUID = 9023342310073836596L;
    private int code;
    private T data;
    private String message;
    private String description;

    public BaseResponse(int code, T data, String message, String description) {
        this.code = code;
        this.data = data;
        this.message = message;
        this.description = description;
    }

    public BaseResponse(int code, T data) {
        this(code, data, "", "");
    }

    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), null, errorCode.getMessage(), errorCode.getDescription());
    }

    public BaseResponse(int code, String message, String description) {
        this(code, null, message, description);
    }
}
