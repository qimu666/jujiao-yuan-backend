package com.qimu.jujiao.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * @Author: QiMu
 * @Date: 2023年03月21日 08:02
 * @Version: 1.0
 * @Description: 字符串工具类
 */
public class StringUtils {
    /**
     * 字符串json数组转Long类型set集合
     *
     * @param jsonList
     * @return Set<Long>
     */
    public static Set<Long> stringJsonListToLongSet(String jsonList) {
        Set<Long> set = new Gson().fromJson(jsonList, new TypeToken<Set<Long>>() {
        }.getType());
        return Optional.ofNullable(set).orElse(new HashSet<>());
    }
}
