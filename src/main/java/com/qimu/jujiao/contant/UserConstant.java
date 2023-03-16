package com.qimu.jujiao.contant;

/**
 * @author: QiMu
 * @Date: 2023年01月18日 21:54
 * @Version: 1.0
 * @Description: 用户常量
 */
public interface UserConstant {
    /**
     * 用户登录态键值
     */
    String LOGIN_USER_STATUS = "loginUserStatus";

    /**
     * 用户缓存键名称
     */
    String USER_REDIS_KEY = String.format("jujiaoyuan:user:search:%s", "qimu");

    /**
     * 标签缓存键名称
     */
    String TAGS_REDIS_KEY = String.format("jujiaoyuan:tags:%s", "tags");

    /**
     * 默认权限
     */
    int DEFAULT_ROLE = 0;

    /**
     * 管理员权限
     */
    int ADMIN_ROLE = 1;

    /**
     * 未登录最大可以看多少条
     */
    int NOT_LONGIN_LOOK_MAX = 10;
}
