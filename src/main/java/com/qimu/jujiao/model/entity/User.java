package com.qimu.jujiao.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户表
 *
 * @author qimu
 */
@TableName(value = "user")
@Data
public class User implements Serializable {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
    /**
     * 用户id
     */
    @TableId(type = IdType.AUTO)
    private long id;
    /**
     * 用户昵称
     */
    private String username;
    /**
     * 账号
     */
    private String userAccount;
    /**
     * 用户头像
     */
    private String userAvatarUrl;
    /**
     * 性别 1 - 男  2-女
     */
    private Integer gender;
    /**
     * 密码
     */
    private String userPassword;
    /**
     * 密码
     */
    private String email;
    /**
     * 联系方式
     */
    private String contactInfo;
    /**
     * 个人简介
     */
    private String userDesc;
    /**
     * 状态 0 - 正常
     */
    private Integer userStatus;
    /**
     * 用户角色 0 - 普通用户 1 - 管理员
     */
    private Integer userRole;
    /**
     * 标签 json 列表
     */
    private String tags;
    /**
     * 队伍id列表
     */
    private String teamIds;
    /**
     * 添加的好友
     */
    private String userIds;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     *
     */
    private Date updateTime;
    /**
     * 是否删除
     */
    private Integer isDelete;
}