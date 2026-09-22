package com.booth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;
import java.util.function.Consumer;

@Data
@TableName("merchant_edit")
@Getter
@Setter
public class MerchantEdit {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联商户主表id */
    private Long merchantId;

    /** 商户负责人姓名 */
    private String name;

    /** 手机号码 */
    private String phone;

    /** 性别 */
    private String gender;

    /** 出生年月 */
    private String birthDate;

    /** 籍贯 */
    private String birthPlace;

    /** 商品品类(多选逗号分隔) */
    private String categories;

    /** 营业执照号码 */
    private String licenseNo;

    /** 营业执照照片 */
    private String licensePhoto;

    /** 电子邮箱 */
    private String email;

    private String goodsInfo;

    private String priceInfo;

    /** 审核状态：1待审核，2审核通过，3审核驳回 */
    private Integer auditStatus;


    /** 驳回原因 */
    private String rejectReason;

    /** 审核管理员ID */
    private Long auditAdminId;

    /** 审核时间 */
    private LocalDateTime auditTime;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;


}

