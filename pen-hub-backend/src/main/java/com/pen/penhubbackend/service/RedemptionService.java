package com.pen.penhubbackend.service;

import com.pen.penhubbackend.model.dto.redemption.RedemptionCodeCreateRequest;
import com.pen.penhubbackend.model.dto.redemption.RedemptionRequest;
import com.pen.penhubbackend.model.entity.RedemptionCode;
import com.pen.penhubbackend.model.entity.RedemptionRecord;
import com.pen.penhubbackend.model.vo.RedemptionCodeVO;
import com.pen.penhubbackend.model.vo.RedemptionRecordVO;

import java.util.List;

/**
 * 兑换码服务接口
 */
public interface RedemptionService {

    /**
     * 管理员创建兑换码
     *
     * @param request 创建请求
     * @return 创建的兑换码列表
     */
    List<RedemptionCodeVO> createRedemptionCodes(RedemptionCodeCreateRequest request);

    /**
     * 用户兑换会员
     *
     * @param userId  用户ID
     * @param request 兑换请求
     * @return 兑换记录
     */
    RedemptionRecordVO redeemCode(Long userId, RedemptionRequest request);

    /**
     * 查询用户的兑换记录
     *
     * @param userId 用户ID
     * @return 兑换记录列表
     */
    List<RedemptionRecordVO> getUserRedemptionRecords(Long userId);

    /**
     * 查询所有兑换记录（管理员）
     *
     * @return 兑换记录列表
     */
    List<RedemptionRecordVO> getAllRedemptionRecords();

    /**
     * 查询所有兑换码（管理员）
     *
     * @return 兑换码列表
     */
    List<RedemptionCodeVO> getAllRedemptionCodes();

    /**
     * 检查并处理过期会员
     */
    void checkAndExpireVipMembers();
}
