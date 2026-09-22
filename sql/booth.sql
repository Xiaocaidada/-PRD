create database if not exists booth;
use booth;

create table booth.admin
(
    id         bigint auto_increment
        primary key,
    username   varchar(50)                        not null comment '用户名',
    password   varchar(100)                       not null comment '密码(MD5+盐)',
    real_name  varchar(50)                        null comment '姓名',
    created_at datetime default CURRENT_TIMESTAMP null,
    constraint uk_username
        unique (username)
)
    comment '管理员' engine = InnoDB
                     charset = utf8mb4;


create table booth.article_category
(
    id            bigint auto_increment
        primary key,
    name          varchar(50)                        not null comment '分类名称',
    fixed_on_home tinyint  default 0                 null comment '是否新闻中心固定展示(0/1)',
    url           varchar(255)                       null comment '自动生成分类页URL',
    created_at    datetime default CURRENT_TIMESTAMP null
)
    comment '文章分类' engine = InnoDB
                       charset = utf8mb4;

create table booth.banner
(
    id         bigint auto_increment
        primary key,
    title      varchar(100)                          null,
    image_url  varchar(255)                          not null comment '图片地址(3:1)',
    link_url   varchar(255)                          null comment '点击跳转链接',
    sort       int         default 0                 null,
    status     varchar(20) default 'ON'              null comment 'ON上架/HIDDEN隐藏',
    created_at datetime    default CURRENT_TIMESTAMP null
)
    comment 'Banner' engine = InnoDB
                     charset = utf8mb4;



create table booth.layout_template
(
    id            bigint auto_increment
        primary key,
    name          varchar(100)                          not null comment '模板名称',
    canvas_width  int         default 210               null comment '画布宽度(mm)',
    canvas_height int         default 297               null comment '画布高度(mm)',
    grid_size     int         default 5                 null comment '网格大小(mm)',
    background    longtext                              null comment '背景base64',
    elements      longtext                              null comment '元素JSON',
    status        varchar(20) default 'ENABLED'         null comment '状态',
    created_at    datetime    default CURRENT_TIMESTAMP null,
    updated_at    datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP
)
    comment '布局模板' engine = InnoDB
                       charset = utf8mb4
                       row_format = DYNAMIC;



create table booth.market
(
    id                    bigint auto_increment
        primary key,
    name                  varchar(30)                        not null comment '市集名称',
    location              varchar(200)                       not null comment '举办地点',
    start_date            date                               not null comment '开始日期',
    end_date              date                               not null comment '结束日期',
    bid_deadline          datetime                           null comment '竞标报名截止时间',
    statement             text                               null comment '市集说明',
    template_id           bigint                             null comment '关联布局模板ID',
    layout_snapshot       longtext                           null comment '布局快照',
    bid_enabled           tinyint  default 0                 null comment '竞标开关 0关/1开',
    status                varchar(20)                        null comment '状态',
    offline_before_status varchar(20)                        null comment '下线前状态',
    created_at            datetime default CURRENT_TIMESTAMP null,
    updated_at            datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP
)
    comment '市集' engine = InnoDB
                   charset = utf8mb4
                   row_format = DYNAMIC;


create table booth.market_bid
(
    id          bigint auto_increment
        primary key,
    market_id   bigint                                not null comment '市集ID',
    merchant_id bigint                                not null comment '商户ID',
    stall_no    varchar(20)                           not null comment '竞标摊位编号(对应模板摊位)',
    remark      varchar(200)                          null comment '竞标备注',
    status      varchar(20) default 'PENDING'         null comment 'PENDING待审核/',
    created_at  datetime    default CURRENT_TIMESTAMP null,
    constraint uk_market_merchant_stall
        unique (market_id, merchant_id, stall_no)
)
    comment '市集竞标记录' engine = InnoDB
                           charset = utf8mb4
                           row_format = DYNAMIC;

create index idx_market
    on booth.market_bid (market_id);


create table booth.material
(
    id            bigint auto_increment
        primary key,
    merchant_id   bigint                                not null,
    module        varchar(20)                           not null comment 'SOURCE商品货源/CULTURE文创内容/STANDARD生产标准/STALL文创市集摊位',
    content       text                                  not null comment '文本描述',
    file_path     json                                  null comment '附件路径',
    status        varchar(20) default 'PENDING'         null comment 'PENDING待审核/APPROVED已通过/REJECTED已驳回',
    reject_reason varchar(255)                          null comment '驳回原因',
    created_at    datetime    default CURRENT_TIMESTAMP null,
    reviewed_at   datetime                              null,
    market_id     int                                   null
)
    comment '补充材料' engine = InnoDB
                       charset = utf8mb4;

create index idx_merchant
    on booth.material (merchant_id);




create table booth.merchant
(
    id                bigint auto_increment
        primary key,
    name              varchar(50)                           not null comment '商户负责人姓名',
    categories        varchar(255)                          null comment '商品品类(多选逗号分隔): 食品/出版物/日用品/服装/酒类',
    license_no        varchar(50)                           not null comment '营业执照号码(唯一标识)',
    phone             varchar(20)                           not null comment '手机号码',
    email             varchar(100)                          null comment '电子邮箱(选填)',
    password          varchar(100)                          not null comment '密码(MD5+盐)',
    status            varchar(20) default 'PENDING'         null comment 'PENDING待审核/APPROVED已通过(合格商家)/REJECTED已驳回',
    reject_reason     varchar(255)                          null comment '驳回原因',
    gender            varchar(10)                           null comment '性别',
    birth_date        varchar(20)                           null comment '出生年月',
    birth_place       varchar(50)                           null comment '籍贯',
    license_photo     varchar(255)                          null comment '营业执照照片',
    goods_info        varchar(1000)                         null comment '商品信息(管理员维护)',
    price_info        varchar(1000)                         null comment '价格信息(管理员维护)',
    assigned_stall_id bigint                                null comment '分配摊位ID',
    created_at        datetime    default CURRENT_TIMESTAMP null,
    updated_at        datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    constraint uk_license_no
        unique (license_no),
    constraint uk_phone
        unique (phone)
)
    comment '商户' engine = InnoDB
                   charset = utf8mb4;





create table booth.merchant_edit
(
    id             bigint auto_increment comment '主键ID'
        primary key,
    merchant_id    bigint                             not null comment '关联商户主表merchant的id',
    name           varchar(64)                        null comment '商户负责人姓名',
    phone          varchar(20)                        null comment '手机号码',
    gender         varchar(10)                        null comment '性别',
    birth_date     varchar(20)                        null comment '出生年月',
    birth_place    varchar(128)                       null comment '籍贯',
    categories     varchar(255)                       null comment '商品品类(逗号分隔)',
    license_no     varchar(64)                        null comment '营业执照号码',
    license_photo  varchar(255)                       null comment '营业执照照片路径',
    email          varchar(128)                       null comment '电子邮箱',
    audit_status   tinyint  default 1                 not null comment '审核状态：1待审核，2审核通过，3审核驳回',
    reject_reason  varchar(512)                       null comment '驳回原因',
    audit_admin_id bigint                             null comment '审核管理员ID',
    audit_time     datetime                           null comment '审核操作时间',
    created_at     datetime default CURRENT_TIMESTAMP not null comment '申请提交时间',
    updated_at     datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    goods_info     varchar(255)                       null comment '商品信息',
    price_info     varchar(255)                       null comment '商品价格'
)
    comment '商户信息修改申请表' engine = InnoDB
                                 charset = utf8mb4;

create index idx_audit_status
    on booth.merchant_edit (audit_status);

create index idx_merchant_id
    on booth.merchant_edit (merchant_id);


create table booth.nav_item
(
    id         bigint auto_increment
        primary key,
    parent_id  bigint   default 0                 null comment '父级ID,0为一级栏目',
    name       varchar(20)                        not null comment '栏目名称(一级限10字符)',
    sort       int      default 0                 null comment '排序',
    url        varchar(255)                       null comment '跳转地址',
    created_at datetime default CURRENT_TIMESTAMP null
)
    comment '动态导航' engine = InnoDB
                       charset = utf8mb4;


create table booth.news
(
    id                bigint auto_increment
        primary key,
    title             varchar(200)                          not null comment '活动标题',
    category_id       bigint                                null comment '分类ID',
    summary           varchar(500)                          null comment '摘要',
    content           text                                  null comment '活动详情',
    cover_url         varchar(255)                          null comment '封面图',
    is_important      tinyint     default 0                 null comment '是否重要通知(0/1)',
    status            varchar(20) default 'PUBLISHED'       null comment 'PUBLISHED已发布/OFFLINE已下线',
    activity_time     varchar(100)                          null comment '活动时间',
    activity_location varchar(200)                          null comment '活动地点',
    register_deadline varchar(100)                          null comment '报名截止时间',
    created_at        datetime    default CURRENT_TIMESTAMP null,
    updated_at        datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP
)
    comment '新闻/活动' engine = InnoDB
                        charset = utf8mb4;



create table booth.`order`
(
    id            bigint auto_increment
        primary key,
    order_no      varchar(50)                              null comment '订单号',
    merchant_id   bigint                                   not null comment '商户ID',
    stall_id      bigint                                   null,
    activity_id   bigint                                   null,
    amount        decimal(10, 2) default 0.00              null comment '成交金额',
    refund_amount decimal(10, 2) default 0.00              null comment '退款金额',
    status        varchar(20)    default 'PAID'            null comment 'PAID已支付/REFUNDED已退款',
    created_at    datetime       default CURRENT_TIMESTAMP null
)
    comment '市集订单' engine = InnoDB
                       charset = utf8mb4;

create index idx_merchant
    on booth.`order` (merchant_id);



create table booth.partner
(
    id         bigint auto_increment
        primary key,
    name       varchar(100)                          not null comment '单位名称',
    category   varchar(50)                           not null comment '学术指导单位/学校科普单位/产学研单位',
    sort       int         default 0                 null,
    link_url   varchar(255)                          null comment '跳转链接',
    status     varchar(20) default 'ON'              null comment 'ON展示/OFF隐藏',
    created_at datetime    default CURRENT_TIMESTAMP null
)
    comment '文化合作单位' engine = InnoDB
                           charset = utf8mb4;


create table booth.representative
(
    id         bigint auto_increment
        primary key,
    name       varchar(50)                           not null comment '姓名',
    title      varchar(100)                          null comment '头衔',
    avatar     varchar(255)                          null comment '头像(1:1)',
    sort       int         default 0                 null,
    status     varchar(20) default 'ON'              null comment 'ON展示/OFF隐藏',
    created_at datetime    default CURRENT_TIMESTAMP null
)
    comment '创意创新代表人物' engine = InnoDB
                               charset = utf8mb4;


create table booth.stall
(
    id                   bigint auto_increment
        primary key,
    stall_no             varchar(20)                           not null comment '摊位编号 A1',
    name                 varchar(50)                           null comment '摊位名称',
    level                int         default 1                 null comment '摊位级别(同级别可多选竞标)',
    area                 varchar(100)                          null comment '区域',
    grid_x               int         default 0                 null comment '平面图横坐标',
    grid_y               int         default 0                 null comment '平面图纵坐标',
    status               varchar(20) default 'AVAILABLE'       null comment 'AVAILABLE可用/OCCUPIED已占用',
    occupant_merchant_id bigint                                null comment '占用商户ID',
    created_at           datetime    default CURRENT_TIMESTAMP null,
    constraint uk_stall_no
        unique (stall_no)
)
    comment '摊位' engine = InnoDB
                   charset = utf8mb4;


create table booth.stall_assign_log
(
    id         bigint auto_increment
        primary key,
    operator   varchar(50)                        null,
    note       varchar(255)                       null,
    created_at datetime default CURRENT_TIMESTAMP null
)
    comment '摊位分配记录' engine = InnoDB
                           charset = utf8mb4;


create table booth.stall_bid
(
    id          bigint auto_increment
        primary key,
    merchant_id bigint                             not null,
    stall_id    bigint                             not null,
    created_at  datetime default CURRENT_TIMESTAMP null,
    constraint uk_merchant_stall
        unique (merchant_id, stall_id)
)
    comment '竞标摊位' engine = InnoDB
                       charset = utf8mb4;

create table booth.traffic_stat
(
    id            bigint auto_increment
        primary key,
    activity_id   bigint                             null,
    merchant_id   bigint                             null,
    stall_id      bigint                             null,
    stat_date     date                               null,
    hour_slot     int                                null comment '时段',
    visitor_count int      default 0                 null,
    created_at    datetime default CURRENT_TIMESTAMP null
)
    comment '客流统计' engine = InnoDB
                       charset = utf8mb4;

create index idx_merchant
    on booth.traffic_stat (merchant_id);


