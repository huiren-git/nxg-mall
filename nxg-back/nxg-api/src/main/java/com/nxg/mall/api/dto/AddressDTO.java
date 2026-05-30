package com.nxg.mall.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AddressDTO {

    // 修改时需要传 id，新增时不传
    private Long id;

    @NotBlank(message = "收货人姓名不能为空")
    private String receiverName;

    @NotBlank(message = "收货人手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String receiverPhone;

    @NotBlank(message = "省份不能为空")
    private String province;

    @NotBlank(message = "城市不能为空")
    private String city;

    @NotBlank(message = "区/县不能为空")
    private String district;

    @NotBlank(message = "详细地址不能为空")
    private String detailAddress;

    // 标签（如：家、公司、学校），可选
    private String label;

    @NotNull(message = "是否默认状态不能为空")
    private Integer isDefault; // 1-默认，0-非默认
}
