package com.qimu.jujiao;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.qimu.jujiao.mapper.UserMapper;
import com.qimu.jujiao.model.entity.User;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@SpringBootTest
class JujiaoBackendApplicationTests {

    @Resource
    private UserMapper userMapper;

    @Test
    void contextLoads() {
        List<User> users = userMapper.selectList(null);
        users.forEach(System.out::println);
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
    @Test
    void update() {
        User user = userMapper.selectById(6);
        Gson gson = new Gson();
        Set<String> oldTags = gson.fromJson(user.getTags(), new TypeToken<Set<String>>() {
        }.getType());

        Set<String> oldTagsCapitalize = toCapitalize(oldTags);
        System.err.println("原本数据=" + oldTagsCapitalize);

        Set<String> newTags = new HashSet<>();
        newTags.add("java");
        newTags.add("女");

        Set<String> newTagsCapitalize = toCapitalize(newTags);
        System.err.println("新的标签=" + newTagsCapitalize);

        // 添加 newTagsCapitalize 中 oldTagsCapitalize 中不存在的元素
        oldTagsCapitalize.addAll(
                newTagsCapitalize.stream()
                        .filter(tag -> !oldTagsCapitalize.contains(tag))
                        .collect(Collectors.toSet())
        );
        // 移除 oldTagsCapitalize 中 newTagsCapitalize 中不存在的元素
        oldTagsCapitalize.removeAll(
                oldTagsCapitalize.stream()
                        .filter(tag -> !newTagsCapitalize.contains(tag))
                        .collect(Collectors.toSet())
        );
        String tagsJson = gson.toJson(oldTagsCapitalize);
        user.setTags(tagsJson);
        userMapper.updateById(user);
    }

    /**
     * 直接删除原来的数据再添加
     */
    @Test
    void updateTag() {
        User user = userMapper.selectById(10);
        Gson gson = new Gson();
        Set<String> oldTags = gson.fromJson(user.getTags(), new TypeToken<Set<String>>() {
        }.getType());

        Set<String> oldTagsCapitalize = toCapitalize(oldTags);
        System.err.println("原本" + oldTagsCapitalize);
        oldTagsCapitalize.clear();

        Set<String> newTags = new HashSet<>();
        newTags.add("java");
        newTags.add("C++");
        newTags.add("男");
        newTags.add("女");
        Set<String> newTagsCapitalize = toCapitalize(newTags);
        System.err.println("新" + newTagsCapitalize);
        oldTagsCapitalize.addAll(newTagsCapitalize);
        System.err.println("更新后" + oldTagsCapitalize);
    }
}
