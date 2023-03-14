package com.qimu.jujiao.exception;

import com.qimu.jujiao.common.ErrorCode;

/**
 * @author: QiMu.
 * @Date: 2023年02月04日 21:14
 * @Version:1.0
 * @Description: 自定义异常类
 */
public class BusinessException extends RuntimeException {
    private static final long serialVersionUID = 5676980599897454498L;
    private final int code;
    private final String description;

    public BusinessException(String message, int code, String description) {
        super(message);
        this.code = code;
        this.description = description;
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.description = errorCode.getDescription();
    }

    public BusinessException(ErrorCode errorCode, String description) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
