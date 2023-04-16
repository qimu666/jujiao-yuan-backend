package com.qimu.jujiao.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 聊天消息表
 *
 * @author qimu
 */
@TableName(value = "chat")
@Data
public class Chat implements Serializable {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
    /**
     * 聊天记录id
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 发送消息id
     */
    private Long fromId;
    /**
     * 接收消息id
     */
    private Long toId;
    /**
     * 队伍聊天室id
     */
    private Long teamId;
    /**
     * 聊天内容
     */
    private String text;
    /**
     * 聊天类型 1-私聊 2-群聊
     */
    private Integer chatType;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     *
     */
    private Date updateTime;
}