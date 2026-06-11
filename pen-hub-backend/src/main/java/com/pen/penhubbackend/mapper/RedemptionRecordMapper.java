package com.pen.penhubbackend.mapper;

import com.mybatisflex.core.BaseMapper;
import com.pen.penhubbackend.model.entity.RedemptionRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 兑换记录数据访问层
 */
@Mapper
public interface RedemptionRecordMapper extends BaseMapper<RedemptionRecord> {
}
