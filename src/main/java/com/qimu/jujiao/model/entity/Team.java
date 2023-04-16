package com.qimu.jujiao.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * 队伍
 *
 * @author qimu
 * @TableName team
 */
@TableName(value = "team")
@Data
public class Team implements Serializable {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 队伍名
     */
    private String teamName;
    /**
     * 队伍头像
     */
    private String teamAvatarUrl;
    /**
     * 队伍加密密码
     */
    private String teamPassword;
    /**
     * 队伍描述
     */
    private String teamDesc;
    /**
     * 最大人数
     */
    private Integer maxNum;
    /**
     * 过期时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date expireTime;
    /**
     * 创建人id
     */
    private Long userId;
    /**
     * 加入队伍的用户id
     */
    private String usersId;
    /**
     * 0 - 公开，1 - 私有，2 - 加密
     */
    private Integer teamStatus;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 更新时间
     */
    private Date updateTime;
    /**
     * 是否删除
     */
    private Integer isDelete;
    /**
     * 公告
     */
    private String announce;
}