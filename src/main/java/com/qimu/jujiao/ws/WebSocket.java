package com.qimu.jujiao.ws;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.google.gson.Gson;
import com.qimu.jujiao.model.entity.Chat;
import com.qimu.jujiao.model.entity.User;
import com.qimu.jujiao.model.request.MessageRequest;
import com.qimu.jujiao.model.vo.MessageVo;
import com.qimu.jujiao.model.vo.WebSocketVo;
import com.qimu.jujiao.service.ChatService;
import com.qimu.jujiao.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

import static com.qimu.jujiao.contant.ChatConstant.PRIVATE_CHAT;
import static com.qimu.jujiao.utils.StringUtils.stringJsonListToLongSet;

@Component
@Slf4j
@ServerEndpoint("/websocket/{userId}")
public class WebSocket {

    private static UserService userService;
    private static ChatService chatService;

    @Resource
    public void setHeatMapService(UserService userService) {
        WebSocket.userService = userService;
    }

    @Resource
    public void setHeatMapService(ChatService chatService) {
        WebSocket.chatService = chatService;
    }

    /**
     * 线程安全的无序的集合
     */
    private static final CopyOnWriteArraySet<Session> SESSIONS = new CopyOnWriteArraySet<>();

    /**
     * 存储在线连接数
     */
    private static final Map<String, Session> SESSION_POOL = new HashMap<>(0);

    @OnOpen
    public void onOpen(Session session, @PathParam(value = "userId") String userId) {
        try {
            if (StringUtils.isBlank(userId) || "undefined".equals(userId)) {
                sendError(userId, "参数有误");
                return;
            }
            SESSIONS.add(session);
            SESSION_POOL.put(userId, session);
            log.info("有新用户加入，userId={}, 当前在线人数为：{}", userId, SESSION_POOL.size());
            sendAllUsers();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendError(String userId, String errorMessage) {
        JSONObject obj = new JSONObject();
        obj.set("error", errorMessage);
        sendOneMessage(userId, obj.toString());
    }

    @OnClose
    public void onClose(@PathParam("userId") String userId, Session session) {
        try {
            if (!SESSION_POOL.isEmpty()) {
                SESSION_POOL.remove(userId);
                SESSIONS.remove(session);
                log.info("【WebSocket消息】连接断开 id为=={}", userId);
                log.info("【WebSocket消息】 session 连接断开 ,id为== {}", session.getId());
            }
            log.info("【WebSocket消息】连接断开，总数为：" + SESSION_POOL.size());
            sendAllUsers();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @OnMessage
    public void onMessage(String message, @PathParam("userId") String userId) {
        if ("PING".equals(message)) {
            sendAllMessage("pong");
            return;
        }
        log.info("服务端收到用户username={}的消息:{}", userId, message);
        MessageRequest messageRequest = new Gson().fromJson(message, MessageRequest.class);
        Long toId = messageRequest.getToId();
        String text = messageRequest.getText();
        Integer chatType = messageRequest.getChatType();
        if (chatType == 1) {
            savaChat(userId, toId, text);
            Session toSession = SESSION_POOL.get(toId.toString());
            if (toSession != null) {
                MessageVo messageVo = chatService.chatResult(Long.parseLong(userId), toId, text);
                String toJson = new Gson().toJson(messageVo);
                sendOneMessage(toId.toString(), toJson);
                log.info("发送给用户username={}，消息：{}", messageVo.getToUser(), toJson);
            } else {
                // sendError(userId, "发送失败");
                log.info("发送失败，未找到用户username={}的session", toId);
            }
        } else {
            MessageVo messageVo = new MessageVo();
            User fromUser = userService.getById(userId);
            WebSocketVo fromWebSocketVo = new WebSocketVo();
            BeanUtils.copyProperties(fromUser, fromWebSocketVo);
            messageVo.setFormUser(fromWebSocketVo);
            messageVo.setText(text);
            String toJson = new Gson().toJson(messageVo);
            sendAllMessage(toJson);
        }
    }

    private void savaChat(String userId, Long toId, String text) {
        User user = userService.getById(userId);
        Set<Long> userIds = stringJsonListToLongSet(user.getUserIds());
        if (!userIds.contains(toId)) {
            sendError(userId, "该用户不是你的好友");
            return;
        }
        Chat chat = new Chat();
        chat.setFromId(Long.parseLong(userId));
        chat.setToId(toId);
        chat.setText(text);
        chat.setChatType(PRIVATE_CHAT);
        chat.setCreateTime(new Date());
        chatService.save(chat);
    }

    /**
     * 此为广播消息
     *
     * @param message 消息
     */
    public void sendAllMessage(String message) {
        log.info("【WebSocket消息】广播消息：" + message);
        for (Session session : SESSIONS) {
            try {
                if (session.isOpen()) {
                    synchronized (session) {
                        session.getBasicRemote().sendText(message);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 此为单点消息
     *
     * @param userId  用户编号
     * @param message 消息
     */
    public void sendOneMessage(String userId, String message) {
        Session session = SESSION_POOL.get(userId);
        if (session != null && session.isOpen()) {
            try {
                synchronized (session) {
                    log.info("【WebSocket消息】单点消息：" + message);
                    session.getAsyncRemote().sendText(message);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 发送所有在线用户信息
     */
    public void sendAllUsers() {
        log.info("【WebSocket消息】发送所有在线用户信息");
        HashMap<String, List<WebSocketVo>> stringListHashMap = new HashMap<>();
        List<WebSocketVo> webSocketVos = new ArrayList<>();
        stringListHashMap.put("users", webSocketVos);
        for (Serializable key : SESSION_POOL.keySet()) {
            User user = userService.getById(key);
            WebSocketVo webSocketVo = new WebSocketVo();
            BeanUtils.copyProperties(user, webSocketVo);
            webSocketVos.add(webSocketVo);
        }
        sendAllMessage(JSONUtil.toJsonStr(stringListHashMap));
    }
}
