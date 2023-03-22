package com.qimu.jujiao.controller;

import cn.hutool.core.util.RandomUtil;
import com.qimu.jujiao.common.BaseResponse;
import com.qimu.jujiao.common.ErrorCode;
import com.qimu.jujiao.common.ResultUtil;
import com.qimu.jujiao.exception.BusinessException;
import com.qimu.jujiao.model.entity.User;
import com.qimu.jujiao.model.request.UpdateTagRequest;
import com.qimu.jujiao.model.request.UserLoginRequest;
import com.qimu.jujiao.model.request.UserRegisterRequest;
import com.qimu.jujiao.model.request.UserUpdatePassword;
import com.qimu.jujiao.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.qimu.jujiao.contant.UserConstant.LOGIN_USER_STATUS;

/**
 * @Author: QiMu
 * @Date: 2023年03月08日 23:21
 * @Version: 1.0
 * @Description:
 */
@RestController
@Slf4j
@RequestMapping("/user")
public class UserController {
    @Resource
    private UserService userService;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @PostMapping("/login")
    public BaseResponse<User> login(@RequestBody UserLoginRequest loginRequest, HttpServletRequest request) {
        if (loginRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = loginRequest.getUserAccount();
        String userPassword = loginRequest.getUserPassword();
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.userLogin(userAccount, userPassword, request);
        return ResultUtil.success(user, "登陆成功");
    }

    @PostMapping("/register")
    public BaseResponse<Long> register(@RequestBody UserRegisterRequest registerRequest) {
        if (registerRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String username = registerRequest.getUsername();
        String userAccount = registerRequest.getUserAccount();
        String userPassword = registerRequest.getUserPassword();
        String checkPassword = registerRequest.getCheckPassword();
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long result = userService.userRegistration(username, userAccount, userPassword, checkPassword);
        return ResultUtil.success(result, "注册成功");
    }

    @GetMapping("/current")
    public BaseResponse<User> getCurrentUser(HttpServletRequest request) {
        User currentUser = userService.getLoginUser(request);
        Long userId = currentUser.getId();
        User user = userService.getById(userId);
        return ResultUtil.success(userService.getSafetyUser(user));
    }

    @GetMapping("/search")
    public BaseResponse<List<User>> searchList(HttpServletRequest request) {
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        // 如果有缓存，直接读缓存
        User loginUser = (User) request.getSession().getAttribute(LOGIN_USER_STATUS);
        List<User> userList = (List<User>) valueOperations.get(userService.redisFormat(loginUser.getId()));
        if (userList != null) {
            return ResultUtil.success(userList);
        }
        List<User> list = userService.list();
        List<User> result = list.stream().map(user -> userService.getSafetyUser(user)).collect(Collectors.toList());
        try {
            redisTemplate.opsForValue().set(userService.redisFormat(loginUser.getId()), result, 1 + RandomUtil.randomInt(1, 4), TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("redis set key error", e);
        }
        return ResultUtil.success(result);
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteUser(Long id, HttpServletRequest request) {
        if (!userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "无权限");
        }
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean remove = userService.removeById(id);
        if (remove) {
            User loginUser = (User) request.getSession().getAttribute(LOGIN_USER_STATUS);
            redisTemplate.delete(userService.redisFormat(loginUser.getId()));
        }
        return ResultUtil.success(remove);
    }

    @PostMapping("/loginOut")
    public BaseResponse<Integer> loginOut(HttpServletRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return ResultUtil.success(userService.loginOut(request));
    }

    @GetMapping("/{id}")
    public BaseResponse<User> getUserById(@PathVariable("id") Integer id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = this.userService.getById(id);
        return ResultUtil.success(user);
    }

    @GetMapping("/search/tags")
    public BaseResponse<List<User>> searchUsersByTags(@RequestParam(required = false) Set<String> tagNameList) {
        if (CollectionUtils.isEmpty(tagNameList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签参数错误");
        }
        List<User> userList = userService.searchUserByTags(tagNameList);
        return ResultUtil.success(userList);
    }

    @PostMapping("/update")
    public BaseResponse<Integer> getUpdateUserById(@RequestBody User user, HttpServletRequest request) {
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User currentUser = userService.getLoginUser(request);
        int updateId = userService.updateUser(user, currentUser);
        redisTemplate.delete(userService.redisFormat(currentUser.getId()));
        return ResultUtil.success(updateId);
    }

    @PostMapping("/update/tags")
    public BaseResponse<Integer> updateTagById(@RequestBody UpdateTagRequest tagRequest, HttpServletRequest request) {
        if (tagRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User currentUser = userService.getLoginUser(request);
        int updateTag = userService.updateTagById(tagRequest, currentUser);
        redisTemplate.delete(userService.redisFormat(currentUser.getId()));
        return ResultUtil.success(updateTag);
    }

    @PostMapping("/update/password")
    public BaseResponse<Integer> updatePassword(@RequestBody UserUpdatePassword updatePassword, HttpServletRequest request) {
        if (updatePassword == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User currentUser = userService.getLoginUser(request);
        int updateTag = userService.updatePasswordById(updatePassword, currentUser);
        redisTemplate.delete(userService.redisFormat(currentUser.getId()));
        return ResultUtil.success(updateTag);
    }
}
