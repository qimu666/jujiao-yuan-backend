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
    teamStatus    int      default 0                 not null comment '0 - 公开，1 - 私有，2 - 加密',
    createTime    datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updateTime    datetime default CURRENT_TIMESTAMP null comment '更新时间',
    isDelete      tinyint  default 0                 not null comment '是否删除',
    announce      varchar(512)                       null comment '公告',
    usersId       varchar(1024)                              null comment '加入队伍的用户id'
)
    comment '队伍';


-- auto-generated definition
create table user
(
    id            bigint auto_increment comment '用户id'
        primary key,
    username      varchar(256)                       null comment '用户昵称',
    userAccount   varchar(256)                       not null comment '账号',
    userAvatarUrl varchar(1024)                      null comment '用户头像',
    gender        tinyint                            null comment '性别 1 - 男  2-女',
    userPassword  varchar(512)                       not null comment '密码',
    contactInfo   varchar(512)                       null comment '联系方式',
    userDesc      varchar(512)                       null comment '个人简介',
    userStatus    int      default 0                 not null comment '状态 0 - 正常',
    userRole      int      default 0                 not null comment '用户角色 0 - 普通用户 1 - 管理员',
    tags          varchar(1024)                      null comment '标签 json 列表',
    teamIds       varchar(1024)                       null comment '队伍id列表',
    userIds       varchar(1024)                       null comment '添加的好友',
    createTime    datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updateTime    datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    isDelete      tinyint  default 0                 not null comment '是否删除',
    email         varchar(128)                       null comment '邮箱'
)
    comment '用户表';

