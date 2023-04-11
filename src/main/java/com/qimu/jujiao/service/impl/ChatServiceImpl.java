package com.qimu.jujiao.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qimu.jujiao.common.ErrorCode;
import com.qimu.jujiao.exception.BusinessException;
import com.qimu.jujiao.mapper.ChatMapper;
import com.qimu.jujiao.model.entity.Chat;
import com.qimu.jujiao.model.entity.User;
import com.qimu.jujiao.model.request.ChatRequest;
import com.qimu.jujiao.model.vo.MessageVo;
import com.qimu.jujiao.model.vo.WebSocketVo;
import com.qimu.jujiao.service.ChatService;
import com.qimu.jujiao.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author qimu
 * @description 针对表【chat(聊天消息表)】的数据库操作Service实现
 * @createDate 2023-04-11 11:19:33
 */
@Service
public class ChatServiceImpl extends ServiceImpl<ChatMapper, Chat>
        implements ChatService {
    @Resource
    private UserService userService;

    @Override
    public List<MessageVo> getPrivateChat(ChatRequest chatRequest, Integer chatType, User loginUser) {
        Long fromId = chatRequest.getFromId();
        Long toId = chatRequest.getToId();
        if (fromId == null || toId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "状态异常请重试");
        }
        LambdaQueryWrapper<Chat> chatLambdaQueryWrapper = new LambdaQueryWrapper<>();
        chatLambdaQueryWrapper.
                and(privateChat -> privateChat.eq(Chat::getFromId, fromId).eq(Chat::getToId, toId)
                        .or().
                        eq(Chat::getToId, fromId).eq(Chat::getFromId, toId)
                ).eq(Chat::getChatType, chatType);
        List<Chat> list = this.list(chatLambdaQueryWrapper);
        return list.stream().map(chat -> {
            MessageVo messageVo = chatResult(fromId, toId, chat.getText());
            if (chat.getFromId().equals(loginUser.getId())) {
                messageVo.setType(true);
            }
            return messageVo;
        }).collect(Collectors.toList());
    }

    @Override
    public MessageVo chatResult(Long fromId, Long toId, String text) {
        MessageVo messageVo = new MessageVo();
        User fromUser = userService.getById(fromId);
        User toUser = userService.getById(toId);
        WebSocketVo fromWebSocketVo = new WebSocketVo();
        WebSocketVo toWebSocketVo = new WebSocketVo();
        BeanUtils.copyProperties(fromUser, fromWebSocketVo);
        BeanUtils.copyProperties(toUser, toWebSocketVo);
        messageVo.setFormUser(fromWebSocketVo);
        messageVo.setToUser(toWebSocketVo);
        messageVo.setText(text);
        return messageVo;
    }
}




