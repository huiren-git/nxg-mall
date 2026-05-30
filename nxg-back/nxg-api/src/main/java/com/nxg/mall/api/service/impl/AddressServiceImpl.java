package com.nxg.mall.api.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nxg.mall.api.dto.AddressDTO;
import com.nxg.mall.common.entity.Address;
import com.nxg.mall.api.mapper.AddressMapper;
import com.nxg.mall.api.service.IAddressService;
import com.nxg.mall.common.exception.BusinessException;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AddressServiceImpl extends ServiceImpl<AddressMapper, Address> implements IAddressService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAddress(Long userId, AddressDTO addressDTO) {
        Address address = new Address();
        // 1. 拷贝前端传来的合法属性
        BeanUtils.copyProperties(addressDTO, address);

        // 2. 强行覆盖关键安全字段，防止越权
        address.setUserId(userId);
        address.setId(null); // 新增时强制自增，防止前端恶意传入 id 变成修改操作

        // 3. 默认地址逻辑控制
        if (Integer.valueOf(1).equals(address.getIsDefault())) {
            resetDefault(userId);
        }

        save(address);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAddress(Long userId, AddressDTO addressDTO) {
        // 安全拦截：防横向越权（校验这个地址是不是当前登录用户的）
        Address oldAddress = this.getById(addressDTO.getId());
        if (oldAddress == null || !oldAddress.getUserId().equals(userId)) {
            throw new BusinessException("您无权修改该地址或地址不存在");
        }

        Address address = new Address();
        BeanUtils.copyProperties(addressDTO, address);
        address.setUserId(userId); // 确保用户只能修改自己的地址

        // 如果设置为默认地址，需重置其它地址
        if (Integer.valueOf(1).equals(address.getIsDefault())) {
            resetDefault(userId);
        }

        updateById(address);
    }

    @Override
    public void deleteAddress(Long userId, Long addressId) {
        // 增加 userId 校验，防止越权删除
        lambdaUpdate()
                .eq(Address::getId, addressId)
                .eq(Address::getUserId, userId)
                .remove();
    }

    @Override
    public List<Address> listMyAddresses(Long userId) {
        return lambdaQuery()
                .eq(Address::getUserId, userId)
                .orderByDesc(Address::getIsDefault) // 默认地址排在最前面
                .orderByDesc(Address::getId)
                .list();
    }

    @Override
    @Transactional
    public void setDefault(Long userId, Long addressId) {
        // 1. 全部重置为 0
        resetDefault(userId);
        // 2. 设置目标地址为 1
        lambdaUpdate()
                .set(Address::getIsDefault, 1)
                .eq(Address::getId, addressId)
                .eq(Address::getUserId, userId)
                .update();
    }

    private void resetDefault(Long userId) {
        lambdaUpdate()
                .set(Address::getIsDefault, 0)
                .eq(Address::getUserId, userId)
                .update();
    }
}
