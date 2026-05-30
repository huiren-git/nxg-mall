package com.nxg.mall.api.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.nxg.mall.api.vo.UserBaseInfoVO;
import com.nxg.mall.common.dto.CustomerQueryDTO;
import com.nxg.mall.common.dto.SalesAddDTO;
import com.nxg.mall.common.dto.SalesUpdateDTO;
import com.nxg.mall.common.entity.User;
import com.nxg.mall.common.vo.CustomerVO;
import com.nxg.mall.common.vo.ManageVO;
import com.nxg.mall.common.vo.UserVO;

import java.io.Serializable;

public interface IUserService extends IService<User> {

    /*
    * 根据用户名判断用户是否存在
    * */
    boolean existsByUsername(String username);

    /*
     * 根据邮箱判断用户是否存在
     * */
    boolean existsByEmail(String email);

    void saveSales(SalesAddDTO dto);

    void updateSales(Long id, SalesUpdateDTO dto);

    void resetPassword(Long id, String newPassword);

    boolean removeById(Serializable id);

    Page<CustomerVO> getCustomerPage(CustomerQueryDTO queryDTO);

    IPage<ManageVO> getManagePage(Integer page, Integer size, String name, Integer role);

    boolean resetCustomerPassword(Long id);

    UserVO getUserVOById(Long id);

    /**
     * 根据用户ID获取顾客的基本信息以及登录轨迹
     *
     * @param userId 用户ID
     * @return 包含基本信息和轨迹的VO对象
     */
    UserBaseInfoVO getCustomerBaseInfo(Long userId);
}