package com.nxg.mall.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nxg.mall.api.dto.AddressDTO;
import com.nxg.mall.common.entity.Address;

import java.util.List;

public interface IAddressService extends IService<Address> {
    /*
    * 新增收货地址
    * */
    void saveAddress(Long userId, AddressDTO addressDTO);

    /*
    * 修改收货地址
    * */
    void updateAddress(Long userId, AddressDTO addressDTO);

    /*
    * 删除收货地址
    * */
    void deleteAddress(Long userId, Long addressId);

    /*
    * 获取收货地址列表
    * */
    List<Address> listMyAddresses(Long userId);

    /*
    * 设置默认收货地址
    * */
    void setDefault(Long userId, Long addressId);
}
