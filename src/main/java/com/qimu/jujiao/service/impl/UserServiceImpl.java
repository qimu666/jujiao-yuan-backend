package com.qimu.jujiao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.qimu.jujiao.common.ErrorCode;
import com.qimu.jujiao.exception.BusinessException;
import com.qimu.jujiao.mapper.UserMapper;
import com.qimu.jujiao.model.entity.User;
import com.qimu.jujiao.model.request.UpdateTagRequest;
import com.qimu.jujiao.model.request.UserQueryRequest;
import com.qimu.jujiao.model.request.UserUpdatePassword;
import com.qimu.jujiao.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static com.qimu.jujiao.contant.UserConstant.ADMIN_ROLE;
import static com.qimu.jujiao.contant.UserConstant.LOGIN_USER_STATUS;
import static com.qimu.jujiao.utils.StringUtils.stringJsonListToLongSet;


/**
 * @author qimu
 * @description 针对表【user(用户表)】的数据库操作Service实现
 * @createDate 2023-01-13 23:17:21
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    /**
     * 盐值 (混淆密码)
     */
    private static final String SALT = "qimu";
    @Resource
    private UserMapper userMapper;

    /**
     * 用户账号注册
     *
     * @param userAccount   用户账号
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 成功的条数
     */
    @Override
    public long userRegistration(String username, String userAccount, String userPassword, String checkPassword) {
        // 1. 非空
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "输入不能为空");
        }
        // 2. 账户长度不小于4位
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号不能小于4位");
        }
        if (!StringUtils.isAnyBlank(username) && username.length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "昵称不能超过20个字符");
        }
        // 2. 账户长度不大于16位
        if (userAccount.length() > 16) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号不能能大于16位");
        }
        // 3. 密码就不小于8位吧
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码小于8位");
        }
        //  5. 账户不包含特殊字符
        // 匹配由数字、小写字母、大写字母组成的字符串,且字符串的长度至少为1个字符
        String pattern = "[0-9a-zA-Z]+";
        if (!userAccount.matches(pattern)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号包含特殊字符");
        }
        // 6. 密码和校验密码相同
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "输入密码不一致");
        }

        // 4. 账户不能重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        long count = this.count(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号已存在");
        }

        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes(StandardCharsets.UTF_8));
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUsername(username);
        user.setTags("[]");
        user.setTeamIds("[]");
        user.setUserIds("[]");

        boolean saveResult = this.save(user);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "注册失败 ");
        }
        return user.getId();
    }

    @Override
    public int updatePasswordById(UserUpdatePassword updatePassword, User currentUser) {
        long id = updatePassword.getId();
        String oldPassword = updatePassword.getOldPassword();
        String newPassword = updatePassword.getNewPassword();
        String checkPassword = updatePassword.getCheckPassword();
        if (StringUtils.isAnyBlank(oldPassword, newPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "输入有误");
        }
        // 密码就不小于8位吧
        if (oldPassword.length() < 8 || checkPassword.length() < 8 || newPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码小于8位");
        }
        // 密码和校验密码相同
        if (!newPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "输入密码不一致");
        }
        if (!isAdmin(currentUser) && currentUser.getId() != id) {
            throw new BusinessException(ErrorCode.NO_AUTH, "权限不足");
        }

        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + oldPassword).getBytes(StandardCharsets.UTF_8));
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.eq("userAccount", currentUser.getUserAccount());
        userQueryWrapper.eq("userPassword", encryptPassword);
        User user = this.getOne(userQueryWrapper);
        // 用户不存在
        if (user == null) {
            log.info("user login failed,userAccount cannot match userPassword");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }

        String newEncryptPassword = DigestUtils.md5DigestAsHex((SALT + newPassword).getBytes(StandardCharsets.UTF_8));

        currentUser.setUserPassword(newEncryptPassword);

        return userMapper.updateById(currentUser);
    }

    @Override
    public String redisFormat(Long key) {
        return String.format("jujiaoyuan:user:search:%s", key);
    }

    @Override
    public List<User> userQuery(UserQueryRequest userQueryRequest, HttpServletRequest request) {
        isLogin(request);
        String searchText = userQueryRequest.getSearchText();
        LambdaQueryWrapper<User> userLambdaQueryWrapper = new LambdaQueryWrapper<>();
        userLambdaQueryWrapper.like(User::getUsername, searchText).or().like(User::getUserDesc, searchText);
        return this.list(userLambdaQueryWrapper);
    }

    @Override
    public List<User> getFriendsById(User currentUser) {
        User loginUser = this.getById(currentUser.getId());
        Set<Long> friendsId = stringJsonListToLongSet(loginUser.getUserIds());
        return friendsId.stream().map(user -> this.getSafetyUser(this.getById(user))).collect(Collectors.toList());
    }

    @Override
    public boolean deleteFriend(User currentUser, Long id) {
        User loginUser = this.getById(currentUser.getId());
        User friendUser = this.getById(id);
        Set<Long> friendsId = stringJsonListToLongSet(loginUser.getUserIds());
        Set<Long> fid = stringJsonListToLongSet(friendUser.getUserIds());
        friendsId.remove(id);
        fid.remove(loginUser.getId());
        String friends = new Gson().toJson(friendsId);
        String fids = new Gson().toJson(fid);
        loginUser.setUserIds(friends);
        friendUser.setUserIds(fids);
        return this.updateById(loginUser) && this.updateById(friendUser);
    }

    @Override
    public User userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 非空
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "输入不能为空");
        }
        // 2. 账户长度不小于4位
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号不能小于4位");
        }
        // 2. 账户长度不大于16位
        if (userAccount.length() > 16) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号不能大于16位");
        }
        // 3. 密码就不小于8位吧
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码小于8位 ");
        }
        //  5. 账户不包含特殊字符
        String pattern = "[0-9a-zA-Z]+";
        if (!userAccount.matches(pattern)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号不能包含特殊字符");
        }

        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes(StandardCharsets.UTF_8));
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.eq("userAccount", userAccount);
        userQueryWrapper.eq("userPassword", encryptPassword);
        User user = this.getOne(userQueryWrapper);

        // 用户不存在
        if (user == null) {
            log.info("user login failed,userAccount cannot match userPassword");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名或密码错误 ");
        }

        // 用户脱敏
        User safeUser = getSafetyUser(user);
        // 记录用户的登录态
        request.getSession().setAttribute(LOGIN_USER_STATUS, safeUser);
        return safeUser;
    }

    @Override
    public List<User> searchFriend(UserQueryRequest userQueryRequest, User currentUser) {
        String searchText = userQueryRequest.getSearchText();
        User user = this.getById(currentUser.getId());
        Set<Long> friendsId = stringJsonListToLongSet(user.getUserIds());
        List<User> users = new ArrayList<>();
        Collections.shuffle(users);
        friendsId.forEach(id -> {
            User u = this.getById(id);
            if (u.getUsername().contains(searchText)) {
                users.add(u);
            }
        });
        return users;
    }

    /**
     * 用户脱敏
     *
     * @param originUser 用户信息
     * @return 脱敏后的用户信息
     */
    @Override
    public User getSafetyUser(User originUser) {
        if (originUser == null) {
            return null;
        }
        User safeUser = new User();
        safeUser.setId(originUser.getId());
        safeUser.setUsername(originUser.getUsername());
        safeUser.setUserAccount(originUser.getUserAccount());
        safeUser.setUserAvatarUrl(originUser.getUserAvatarUrl());
        safeUser.setGender(originUser.getGender());
        safeUser.setEmail(originUser.getEmail());
        safeUser.setContactInfo(originUser.getContactInfo());
        safeUser.setUserDesc(originUser.getUserDesc());
        safeUser.setUserStatus(originUser.getUserStatus());
        safeUser.setUserRole(originUser.getUserRole());
        safeUser.setUserIds(originUser.getUserIds());
        safeUser.setTags(originUser.getTags());
        safeUser.setTeamIds(originUser.getTeamIds());
        safeUser.setCreateTime(originUser.getCreateTime());
        return safeUser;
    }

    @Override
    public Integer loginOut(HttpServletRequest request) {
        request.getSession().removeAttribute(LOGIN_USER_STATUS);
        return 1;
    }

    @Override
    public boolean isAdmin(HttpServletRequest request) {
        // 仅管理员可查询
        Object objUser = request.getSession().getAttribute(LOGIN_USER_STATUS);
        User user = (User) objUser;
        return user != null && user.getUserRole() == ADMIN_ROLE;
    }

    @Override
    public boolean isAdmin(User user) {
        return user != null && user.getUserRole() == ADMIN_ROLE;
    }

    /**
     * 根据标签查询用户
     *
     * @param tagNameList 标签列表
     * @return
     */
    @Override
    public List<User> searchUserByTags(Set<String> tagNameList) {
        if (CollectionUtils.isEmpty(tagNameList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 查询出所有的用户
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        List<User> userList = userMapper.selectList(queryWrapper);
        Gson gson = new Gson();
        // 在内存中查询符合要求的标签
        return userList.stream().filter(user -> {
            String tagsStr = user.getTags();
            Set<String> tempTagNameStr = gson.fromJson(tagsStr, new TypeToken<Set<String>>() {
            }.getType());
            // 是否为空，为空返回HashSet的默认值，否则返回数值
            tempTagNameStr = Optional.ofNullable(tempTagNameStr).orElse(new HashSet<>());
            // tempTagNameStr集合中每一个元素首字母转换为大写
            tempTagNameStr = tempTagNameStr.stream().map(StringUtils::capitalize).collect(Collectors.toSet());
            // 返回false会过滤掉
            for (String tagName : tagNameList) {
                tagName = StringUtils.capitalize(tagName);
                if (!tempTagNameStr.contains(tagName)) {
                    return false;
                }
            }
            return true;
        }).map(this::getSafetyUser).collect(Collectors.toList());
    }

    @Override
    public void isLogin(HttpServletRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN, "请先登录");
        }
        Object objUser = request.getSession().getAttribute(LOGIN_USER_STATUS);
        User currentUser = (User) objUser;
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN, "请先登录");
        }
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        Object objUser = request.getSession().getAttribute(LOGIN_USER_STATUS);
        User currentUser = (User) objUser;
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN, "请先登录");
        }
        return currentUser;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateUser(User user, User currentUser) {
        long userId = user.getId();
        if (userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (!StringUtils.isAnyBlank(user.getUserDesc()) && user.getUserDesc().length() > 30) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "简介不能超过30个字符");
        }
        if (!StringUtils.isAnyBlank(user.getUsername()) && user.getUsername().length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "昵称不能超过20个字符");
        }
        if (!StringUtils.isAnyBlank(user.getContactInfo()) && user.getContactInfo().length() > 18) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "联系方式不能超过18个字符");
        }
        // 如果是管理员，允许更新任意用户
        // 如果不是管理员，只允许更新当前（自己的）信息
        if (!isAdmin(currentUser) && userId != currentUser.getId()) {
            throw new BusinessException(ErrorCode.NO_AUTH, "无权限");
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", user.getUserAccount());
        long count = this.count(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号已存在  请重新输入");
        }
        User oldUser = userMapper.selectById(userId);
        if (oldUser == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        return userMapper.updateById(user);
    }


    /**
     * String类型集合首字母大写
     *
     * @param oldSet 原集合
     * @return 首字母大写的集合
     */
    private Set<String> toCapitalize(Set<String> oldSet) {
        return oldSet.stream().map(StringUtils::capitalize).collect(Collectors.toSet());
    }

    /**
     * 流处理
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateTagById(UpdateTagRequest updateTag, User currentUser) {
        long id = updateTag.getId();
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该用户不存在");
        }
        Set<String> newTags = updateTag.getTagList();
        if (newTags.size() > 12) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "最多设置12个标签");
        }
        if (!isAdmin(currentUser) && id != currentUser.getId()) {
            throw new BusinessException(ErrorCode.NO_AUTH, "无权限");
        }
        User user = userMapper.selectById(id);
        Gson gson = new Gson();
        Set<String> oldTags = gson.fromJson(user.getTags(), new TypeToken<Set<String>>() {
        }.getType());
        Set<String> oldTagsCapitalize = toCapitalize(oldTags);
        Set<String> newTagsCapitalize = toCapitalize(newTags);

        // 添加 newTagsCapitalize 中 oldTagsCapitalize 中不存在的元素
        oldTagsCapitalize.addAll(newTagsCapitalize.stream().filter(tag -> !oldTagsCapitalize.contains(tag)).collect(Collectors.toSet()));
        // 移除 oldTagsCapitalize 中 newTagsCapitalize 中不存在的元素
        oldTagsCapitalize.removeAll(oldTagsCapitalize.stream().filter(tag -> !newTagsCapitalize.contains(tag)).collect(Collectors.toSet()));
        String tagsJson = gson.toJson(oldTagsCapitalize);
        user.setTags(tagsJson);
        return userMapper.updateById(user);
    }
}




