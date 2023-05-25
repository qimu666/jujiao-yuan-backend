create schema jujiao_yuan collate utf8mb4_general_ci;
ALTER DATABASE jujiao_yuan CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
use jujiao_yuan;

-- auto-generated definition
create table team
(
    id            bigint auto_increment comment 'id'
        primary key,
    teamName      varchar(256)                       null comment '队伍名',
    teamAvatarUrl varchar(1024)                      null comment '队伍头像',
    teamPassword  varchar(512)                       null comment '队伍加密密码',
    teamDesc      varchar(1024)                      null comment '队伍描述',
    maxNum        int      default 1                 not null comment '最大人数',
    expireTime    datetime                           null comment '过期时间',
    userId        bigint                             null comment '创建人id',
    usersId       varchar(1024)                      null comment '加入队伍的用户id',
    createTime    datetime default CURRENT_TIMESTAMP null comment '创建时间',
    teamStatus    int      default 0                 not null comment '0 - 公开，1 - 私有，2 - 加密',
    isDelete      tinyint  default 0                 not null comment '是否删除',
    announce      varchar(512)                       null comment '队伍公告',
    updateTime    datetime default CURRENT_TIMESTAMP null
)
    comment '队伍' charset = utf8;



INSERT INTO jujiao_yuan.team (id, teamName, teamAvatarUrl, teamPassword, teamDesc, maxNum, expireTime, userId, usersId,
                              createTime, teamStatus, isDelete, announce, updateTime)
VALUES (100001, '加密测试队', 'https://img.qimuu.icu/typory/22.jfif', '6c7bf8e3df13c49a2a11bdcfe9a81f2e', '超级无敌霸气', 5,
        '2023-10-1 19:53:57', 10001, '[10001]', '2023-03-10 23:59:12', 2, 0, '当你对自己诚实的时候，世界上就没有人能够欺骗得了你。',
        '2023-03-23 18:56:23');
INSERT INTO jujiao_yuan.team (id, teamName, teamAvatarUrl, teamPassword, teamDesc, maxNum, expireTime, userId, usersId,
                              createTime, teamStatus, isDelete, announce, updateTime)
VALUES (100002, '公开测试队', 'https://img.qimuu.icu/typory/22.jfif', '6c7bf8e3df13c49a2a11bdcfe9a81f2e', '超级无敌霸气', 5,
        '2023-10-1 19:53:57', 10001, '[10001]', '2023-03-10 23:59:12', 0, 0, '人类最高级别的安慰，就是理解别人的痛苦，并陪伴他。',
        '2023-03-23 18:56:23');


-- auto-generated definition
create table user
(
    id            bigint auto_increment comment '用户id'
        primary key,
    username      varchar(256)                       null comment '用户昵称',
    userAccount   varchar(256)                       not null comment '账号',
    userAvatarUrl varchar(1024)                      null comment '用户头像',
    gender        tinyint                            null comment '性别 1 - 男  2-女',
    userPassword  varchar(512)                       null comment '密码',
    contactInfo   varchar(512)                       null comment '联系方式',
    userDesc      varchar(512)                       null comment '个人简介',
    userStatus    int      default 0                 not null comment '状态 0 - 正常',
    userRole      int      default 0                 not null comment '用户角色 0 - 普通用户 1 - 管理员',
    tags          varchar(1024)                      null comment '标签 json 列表',
    teamIds       varchar(512)                       null comment '队伍id列表',
    userIds       varchar(512)                       null comment '添加的好友',
    createTime    datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updateTime    datetime default CURRENT_TIMESTAMP null,
    isDelete      tinyint  default 0                 not null comment '是否删除',
    email         varchar(128)                       null comment '邮箱'
)
    comment '用户表' charset = utf8;



INSERT INTO jujiao_yuan.user (id, username, userAccount, userAvatarUrl, gender, userPassword, contactInfo, userDesc,
                              userStatus, userRole, tags, teamIds, userIds, createTime, updateTime, isDelete, email)
VALUES (10001, '柒木', 'qimu', 'https://img.qimuu.icu/user_avatar/1/pR5dHavR-好烦噢.jpg', 1,
        '89c7f39d98fb169ffea0698c18831fe1', 'vx:aqimu66', '阿巴阿巴', 0, 1,
        '["C#","Java","C++","找伙伴","女","求职中","男","运动","Python","音乐","上班族"]', '[100002,100001]', '[2,3,4]',
        '2023-03-08 23:46:31', '2023-03-11 00:42:35', 0, '2483482026@qq.com');


-- auto-generated definition
create table friends
(
    id         bigint auto_increment comment '好友申请id'
        primary key,
    fromId     bigint                             not null comment '发送申请的用户id',
    receiveId  bigint                             null comment '接收申请的用户id ',
    isRead     tinyint  default 0                 not null comment '是否已读(0-未读 1-已读)',
    status     tinyint  default 0                 not null comment '申请状态 默认0 （0-未通过 1-已同意 2-已过期 3-已撤销）',
    createTime datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updateTime datetime default CURRENT_TIMESTAMP null,
    isDelete   tinyint  default 0                 not null comment '是否删除',
    remark     varchar(214)                       null comment '好友申请备注信息'
)
    comment '好友申请管理表' charset = utf8mb4;


-- auto-generated definition
create table chat
(
    id         bigint auto_increment comment '聊天记录id'
        primary key,
    fromId     bigint                                  not null comment '发送消息id',
    toId       bigint                                  null comment '接收消息id',
    text       varchar(512) collate utf8mb4_unicode_ci null,
    chatType   tinyint                                 not null comment '聊天类型 1-私聊 2-群聊',
    createTime datetime default CURRENT_TIMESTAMP      null comment '创建时间',
    updateTime datetime default CURRENT_TIMESTAMP      null,
    teamId     bigint                                  null
)
    comment '聊天消息表' charset = utf8mb4;


SET NAMES utf8mb4;

ALTER TABLE chat
    CHARSET = utf8mb4;

ALTER DATABASE jujiao_yuan CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;