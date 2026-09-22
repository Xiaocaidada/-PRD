package com.booth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商户（商家用户）
 * 审核状态: PENDING 待审核 / APPROVED 已通过(合格商家) / REJECTED 已驳回
 */
@Data
@TableName("merchant")
public class Merchant {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商户负责人姓名 */
    private String name;

    /** 商品品类(多选，逗号分隔)：食品、出版物、日用品、服装、酒类 */
    private String categories;

    /** 营业执照号码（唯一标识） */
    private String licenseNo;

    /** 手机号码 */
    private String phone;

    /** 电子邮箱（选填） */
    private String email;

    /** 密码（MD5+盐） */
    private String password;

    /** 审核状态 */
    private String status;

    /** 驳回原因 */
    private String rejectReason;

    /** 性别 */
    private String gender;

    /** 出生年月 */
    private String birthDate;

    /** 籍贯 */
    private String birthPlace;

    /** 营业执照照片 */
    private String licensePhoto;

    /** 商品信息（管理员维护） */
    private String goodsInfo;

    /** 价格信息（管理员维护） */
    private String priceInfo;

    /** 分配到的摊位ID */
    private Long assignedStallId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
