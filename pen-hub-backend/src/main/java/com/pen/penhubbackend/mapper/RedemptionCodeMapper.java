package com.pen.penhubbackend.mapper;

import com.mybatisflex.core.BaseMapper;
import com.pen.penhubbackend.model.entity.RedemptionCode;
import org.apache.ibatis.annotations.Mapper;

/**
 * 兑换码数据访问层
 */
@Mapper
public interface RedemptionCodeMapper extends BaseMapper<RedemptionCode> {
}
