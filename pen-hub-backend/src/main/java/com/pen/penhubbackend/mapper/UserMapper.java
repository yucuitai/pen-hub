package com.pen.penhubbackend.mapper;

import com.mybatisflex.core.BaseMapper;
import com.pen.penhubbackend.model.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户表 Mapper 接口
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

}
