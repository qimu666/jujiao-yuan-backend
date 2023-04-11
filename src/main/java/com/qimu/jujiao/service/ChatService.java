package com.qimu.jujiao.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.qimu.jujiao.model.entity.Chat;
import com.qimu.jujiao.model.entity.User;
import com.qimu.jujiao.model.request.ChatRequest;
import com.qimu.jujiao.model.vo.MessageVo;

import java.util.List;

/**
 * @author qimu
 * @description 针对表【chat(聊天消息表)】的数据库操作Service
 * @createDate 2023-04-11 11:19:33
 */
public interface ChatService extends IService<Chat> {
    /**
     * 获取聊天内容
     *
     * @param chatRequest
     * @param chatType
     * @param loginUser
     * @return
     */
    List<MessageVo> getPrivateChat(ChatRequest chatRequest, Integer chatType, User loginUser);

    /**
     * 聊天记录映射
     *
     * @param fromId
     * @param toId
     * @param text
     * @return
     */
    MessageVo chatResult(Long fromId, Long toId, String text);
}
