package com.campustrade.auth;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

@Mapper
public interface AuthMapper extends BaseMapper<UserAccount> {
}
