package com.qimu.jujiao.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.qimu.jujiao.model.entity.User;
import com.qimu.jujiao.model.request.UpdateTagRequest;
import com.qimu.jujiao.model.request.UserQueryRequest;
import com.qimu.jujiao.model.request.UserUpdatePassword;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;

/**
 * @author qimu
 * @description 针对表【user(用户表)】的数据库操作Service
 * @createDate 2023-01-13 23:17:21
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param username      用户名
     * @param userAccount   用户账号
     * @param userPassword  用户密码
     * @param checkPassword 确认密码
     * @return 新注册用户的id
     */
    long userRegistration(String username, String userAccount, String userPassword, String checkPassword);

    /**
     * 用户登录
     *
     * @param userAccount  用户账号
     * @param userPassword 用户密码
     * @param request      记录用户的登录态
     * @return 登陆成功的用户信息（脱敏之后）
     */
    User userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 用户脱敏
     *
     * @param originUser 用户信息
     * @return 脱敏后的用户信息
     */
    User getSafetyUser(User originUser);

    /**
     * 退出登录
     *
     * @param request
     * @return
     */
    Integer loginOut(HttpServletRequest request);

    /**
     * 是否为管理员
     *
     * @param request
     * @return
     */
    boolean isAdmin(HttpServletRequest request);

    /**
     * 根据标签搜索用户
     *
     * @param tagNameSet
     * @return
     */
    List<User> searchUserByTags(Set<String> tagNameSet);

    /**
     * 获取当前登录信息
     *
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 当前是否登录
     *
     * @param request
     * @return
     */
    void isLogin(HttpServletRequest request);

    /**
     * 修改用户
     *
     * @param user
     * @param currentUser
     * @return
     */
    int updateUser(User user, User currentUser);

    /**
     * 搜索好友
     *
     * @param userQueryRequest
     * @param currentUser
     * @return
     */
    List<User> searchFriend(UserQueryRequest userQueryRequest, User currentUser);

    /**
     * 获取当前用户是否为管理员
     *
     * @param user
     * @return
     */
    boolean isAdmin(User user);

    /**
     * 修改标签
     *
     * @param updateTag   修改标签dto
     * @param currentUser 当前用户
     * @return
     */
    int updateTagById(UpdateTagRequest updateTag, User currentUser);

    /**
     * 修改密码
     *
     * @param updatePassword
     * @param currentUser
     * @return
     */
    int updatePasswordById(UserUpdatePassword updatePassword, User currentUser);

    /**
     * redisKey
     *
     * @param key
     * @return
     */
    String redisFormat(Long key);

    /**
     * 搜索用户
     *
     * @param userQueryRequest
     * @param request
     * @return
     */
    List<User> userQuery(UserQueryRequest userQueryRequest, HttpServletRequest request);

    /**
     * 删除好友
     *
     * @param currentUser
     * @param id
     * @return
     */
    boolean deleteFriend(User currentUser, Long id);

    /**
     * 根据id获取好友列表
     *
     * @param currentUser
     * @return
     */
    List<User> getFriendsById(User currentUser);
}
