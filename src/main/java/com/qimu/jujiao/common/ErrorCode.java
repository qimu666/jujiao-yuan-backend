package com.qimu.jujiao.common;

/**
 * @Author: QiMu
 * @Date: 2023年02月04日 19:53
 * @Version: 1.0
 * @Description: 错误码
 */
public enum ErrorCode {
    /**
     * 成功
     */
    SUCCESS(0, "ok", ""),
    /**
     * 请求参数错误
     */
    PARAMS_ERROR(40000, "请求参数错误", ""),
    /**
     * 请求参数为空
     */
    NULL_ERROR(40001, "请求参数为空", ""),
    /**
     * 未登录
     */
    NOT_LOGIN(40100, "未登录", ""),
    /**
     * 无权限
     */
    NO_AUTH(40101, "无权限", ""),

    /**
     * 无权限(踢出队伍权限)
     */
    KICK_OUT_USER(401001, "无权限踢出", ""),
    /**
     * 系统内部错误
     */
    SYSTEM_ERROR(50000, "系统内部错误", "");
    /**
     * 状态码
     */
    private final int code;
    /**
     * 状态码信息
     */
    private final String message;
    /**
     * 状态码信息(详细)
     */
    private final String description;

    ErrorCode(int code, String message, String description) {
        this.code = code;
        this.message = message;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getDescription() {
        return description;
    }
}
