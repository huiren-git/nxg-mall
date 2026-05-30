package com.nxg.mall.api.controller;

import com.nxg.mall.api.dto.AddressDTO;
import com.nxg.mall.common.Result;
import com.nxg.mall.common.entity.Address;
import com.nxg.mall.api.service.IAddressService;
import com.nxg.mall.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/address")
public class AddressController {

    @Autowired
    private IAddressService addressService;

    // 获取当前用户的所有地址
    @GetMapping()
    public Result<List<Address>> list() {
        return Result.success(addressService.listMyAddresses(SecurityUtils.getUserId()));
    }

    // 新增地址
    @PostMapping
    public Result<Void> add(@Validated @RequestBody AddressDTO addressDTO) {
        addressService.saveAddress(SecurityUtils.getUserId(), addressDTO);
        return Result.success();
    }

    // 修改地址
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Validated @RequestBody AddressDTO addressDTO) {
        // 将路径中的 id 强行赋给 DTO，确保操作对象一致
        addressDTO.setId(id);
        addressService.updateAddress(SecurityUtils.getUserId(), addressDTO);
        return Result.success();
    }

    // 删除地址
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        addressService.deleteAddress(SecurityUtils.getUserId(), id);
        return Result.success();
    }

    // 设为默认地址
    @PutMapping("/default/{id}")
    public Result<Void> setDefault(@PathVariable Long id) {
        addressService.setDefault(SecurityUtils.getUserId(), id);
        return Result.success();
    }
}
