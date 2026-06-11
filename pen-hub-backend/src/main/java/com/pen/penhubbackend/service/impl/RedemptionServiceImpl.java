package com.pen.penhubbackend.service.impl;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.pen.penhubbackend.constant.UserConstant;
import com.pen.penhubbackend.exception.BusinessException;
import com.pen.penhubbackend.exception.ErrorCode;
import com.pen.penhubbackend.mapper.RedemptionCodeMapper;
import com.pen.penhubbackend.mapper.RedemptionRecordMapper;
import com.pen.penhubbackend.mapper.UserMapper;
import com.pen.penhubbackend.model.dto.redemption.RedemptionCodeCreateRequest;
import com.pen.penhubbackend.model.dto.redemption.RedemptionRequest;
import com.pen.penhubbackend.model.entity.RedemptionCode;
import com.pen.penhubbackend.model.entity.RedemptionRecord;
import com.pen.penhubbackend.model.entity.User;
import com.pen.penhubbackend.model.enums.ProductTypeEnum;
import com.pen.penhubbackend.model.enums.RedemptionRecordStatusEnum;
import com.pen.penhubbackend.model.enums.RedemptionStatusEnum;
import com.pen.penhubbackend.model.vo.RedemptionCodeVO;
import com.pen.penhubbackend.model.vo.RedemptionRecordVO;
import com.pen.penhubbackend.service.RedemptionService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 兑换码服务实现
 */
@Service
@Slf4j
public class RedemptionServiceImpl implements RedemptionService {

    /**
     * 会员时长：月度会员（30天）
     */
    private static final int VIP_MONTHLY_DAYS = 30;

    /**
     * 会员时长：年度会员（365天）
     */
    private static final int VIP_YEARLY_DAYS = 365;

    /**
     * 兑换码有效期：一年
     */
    private static final int CODE_EXPIRE_YEARS = 1;

    /**
     * 批量创建兑换码最大数量
     */
    private static final int MAX_BATCH_CREATE_COUNT = 10;

    @Resource
    private RedemptionCodeMapper redemptionCodeMapper;

    @Resource
    private RedemptionRecordMapper redemptionRecordMapper;

    @Resource
    private UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<RedemptionCodeVO> createRedemptionCodes(RedemptionCodeCreateRequest request) {
        // 参数校验
        validateCreateRequest(request);

        // 验证产品类型
        ProductTypeEnum productType = validateProductType(request.getProductType());

        // 创建兑换码
        List<RedemptionCode> codes = new ArrayList<>();
        int count = request.getCount() != null ? request.getCount() : 1;

        for (int i = 0; i < count; i++) {
            RedemptionCode code = RedemptionCode.builder()
                    .code(generateUniqueCode(productType))
                    .productType(productType.getValue())
                    .maxUses(1)
                    .usedCount(0)
                    .status(RedemptionStatusEnum.ACTIVE.getValue())
                    .expireTime(LocalDateTime.now().plusYears(CODE_EXPIRE_YEARS))
                    .description(request.getDescription())
                    .build();
            codes.add(code);
        }

        // 批量插入
        for (RedemptionCode code : codes) {
            redemptionCodeMapper.insert(code);
        }

        log.info("管理员创建兑换码成功, productType={}, count={}", productType.getValue(), codes.size());

        // 转换为 VO
        return codes.stream()
                .map(this::convertToCodeVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RedemptionRecordVO redeemCode(Long userId, RedemptionRequest request) {
        // 参数校验
        if (StrUtil.isBlank(request.getCode())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "兑换码不能为空");
        }

        // 验证用户
        User user = getUserOrThrow(userId);

        // 验证用户是否已是会员
        if (UserConstant.VIP_ROLE.equals(user.getUserRole())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "您已是会员，暂不支持重复兑换");
        }

        // 查询兑换码
        RedemptionCode redemptionCode = findRedemptionCodeByCode(request.getCode());
        if (redemptionCode == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "兑换码不存在");
        }

        // 验证兑换码状态
        validateRedemptionCode(redemptionCode);

        // 计算会员到期时间
        ProductTypeEnum productType = ProductTypeEnum.getByValue(redemptionCode.getProductType());
        LocalDateTime expireTime = calculateExpireTime(productType);

        // 创建兑换记录
        RedemptionRecord record = RedemptionRecord.builder()
                .userId(userId)
                .codeId(redemptionCode.getId())
                .code(redemptionCode.getCode())
                .productType(redemptionCode.getProductType())
                .status(RedemptionRecordStatusEnum.SUCCESS.getValue())
                .expireTime(expireTime)
                .description("兑换" + productType.getDescription())
                .build();
        redemptionRecordMapper.insert(record);

        // 更新兑换码使用次数
        updateRedemptionCodeUsage(redemptionCode);

        // 升级用户会员
        upgradeUserToVip(user.getId(), productType);

        log.info("用户兑换会员成功, userId={}, code={}, productType={}, expireTime={}",
                userId, redemptionCode.getCode(), productType.getValue(), expireTime);

        return convertToRecordVO(record);
    }

    @Override
    public List<RedemptionRecordVO> getUserRedemptionRecords(Long userId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("user_id", userId)
                .orderBy("create_time", false);
        List<RedemptionRecord> records = redemptionRecordMapper.selectListByQuery(queryWrapper);
        return records.stream()
                .map(this::convertToRecordVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<RedemptionRecordVO> getAllRedemptionRecords() {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .orderBy("create_time", false);
        List<RedemptionRecord> records = redemptionRecordMapper.selectListByQuery(queryWrapper);
        return records.stream()
                .map(this::convertToRecordVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<RedemptionCodeVO> getAllRedemptionCodes() {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .orderBy("create_time", false);
        List<RedemptionCode> codes = redemptionCodeMapper.selectListByQuery(queryWrapper);
        return codes.stream()
                .map(this::convertToCodeVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void checkAndExpireVipMembers() {
        LocalDateTime now = LocalDateTime.now();

        // 查询所有已过期的兑换记录
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("status", RedemptionRecordStatusEnum.SUCCESS.getValue())
                .le("expire_time", now);
        List<RedemptionRecord> expiredRecords = redemptionRecordMapper.selectListByQuery(queryWrapper);

        if (expiredRecords.isEmpty()) {
            return;
        }

        log.info("发现 {} 条过期的会员记录，开始处理降级", expiredRecords.size());

        // 按用户ID分组处理
        expiredRecords.stream()
                .collect(Collectors.groupingBy(RedemptionRecord::getUserId))
                .forEach((userId, records) -> {
                    try {
                        downgradeUserToNormal(userId);
                        log.info("用户会员已过期，降级成功, userId={}", userId);
                    } catch (Exception e) {
                        log.error("用户会员降级失败, userId={}", userId, e);
                    }
                });
    }

    // ==================== 私有方法封装 ====================

    /**
     * 校验创建请求
     */
    private void validateCreateRequest(RedemptionCodeCreateRequest request) {
        if (StrUtil.isBlank(request.getProductType())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "产品类型不能为空");
        }
        if (request.getCount() != null && (request.getCount() < 1 || request.getCount() > MAX_BATCH_CREATE_COUNT)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "创建数量必须在1-10之间");
        }
    }

    /**
     * 验证产品类型
     */
    private ProductTypeEnum validateProductType(String productType) {
        ProductTypeEnum productTypeEnum = ProductTypeEnum.getByValue(productType);
        if (productTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "产品类型不存在");
        }
        // 只允许月度会员和年度会员
        if (productTypeEnum == ProductTypeEnum.VIP_PERMANENT) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "永久会员暂不支持兑换码功能");
        }
        return productTypeEnum;
    }

    /**
     * 生成唯一兑换码
     */
    private String generateUniqueCode(ProductTypeEnum productType) {
        String prefix;
        switch (productType) {
            case VIP_MONTHLY:
                prefix = "VIP-M-";
                break;
            case VIP_YEARLY:
                prefix = "VIP-Y-";
                break;
            default:
                prefix = "VIP-";
        }
        // 生成8位随机字符串
        String randomStr = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return prefix + randomStr;
    }

    /**
     * 获取用户或抛出异常
     */
    private User getUserOrThrow(Long userId) {
        User user = userMapper.selectOneById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }
        return user;
    }

    /**
     * 根据兑换码查询
     */
    private RedemptionCode findRedemptionCodeByCode(String code) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("code", code);
        return redemptionCodeMapper.selectOneByQuery(queryWrapper);
    }

    /**
     * 验证兑换码有效性
     */
    private void validateRedemptionCode(RedemptionCode redemptionCode) {
        // 验证状态
        if (!RedemptionStatusEnum.ACTIVE.getValue().equals(redemptionCode.getStatus())) {
            RedemptionStatusEnum statusEnum = RedemptionStatusEnum.getByValue(redemptionCode.getStatus());
            String statusDesc = statusEnum != null ? statusEnum.getDescription() : "未知状态";
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "兑换码" + statusDesc);
        }

        // 验证是否过期
        if (redemptionCode.getExpireTime() != null && LocalDateTime.now().isAfter(redemptionCode.getExpireTime())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "兑换码已过期");
        }

        // 验证是否用完
        if (redemptionCode.getUsedCount() >= redemptionCode.getMaxUses()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "兑换码已被使用");
        }
    }

    /**
     * 计算会员到期时间
     */
    private LocalDateTime calculateExpireTime(ProductTypeEnum productType) {
        LocalDateTime now = LocalDateTime.now();
        switch (productType) {
            case VIP_MONTHLY:
                return now.plusDays(VIP_MONTHLY_DAYS);
            case VIP_YEARLY:
                return now.plusDays(VIP_YEARLY_DAYS);
            case VIP_PERMANENT:
                return null; // 永久会员无到期时间
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的会员类型");
        }
    }

    /**
     * 更新兑换码使用次数
     */
    private void updateRedemptionCodeUsage(RedemptionCode redemptionCode) {
        RedemptionCode update = new RedemptionCode();
        update.setId(redemptionCode.getId());
        update.setUsedCount(redemptionCode.getUsedCount() + 1);

        // 如果使用次数达到上限，更新状态为已用完
        if (update.getUsedCount() >= redemptionCode.getMaxUses()) {
            update.setStatus(RedemptionStatusEnum.EXHAUSTED.getValue());
        }

        redemptionCodeMapper.update(update);
    }

    /**
     * 升级用户为会员
     */
    private void upgradeUserToVip(Long userId, ProductTypeEnum productType) {
        User updateUser = new User();
        updateUser.setId(userId);
        updateUser.setVipTime(LocalDateTime.now());
        updateUser.setVipType(productType.getValue());
        updateUser.setUserRole(UserConstant.VIP_ROLE);
        userMapper.update(updateUser);
    }

    /**
     * 降级用户为普通用户
     */
    private void downgradeUserToNormal(Long userId) {
        User updateUser = new User();
        updateUser.setId(userId);
        updateUser.setVipTime(null);
        updateUser.setVipType(null);
        updateUser.setUserRole(UserConstant.DEFAULT_ROLE);
        userMapper.update(updateUser);
    }

    /**
     * 转换为兑换码VO
     */
    private RedemptionCodeVO convertToCodeVO(RedemptionCode code) {
        RedemptionCodeVO vo = new RedemptionCodeVO();
        vo.setId(code.getId());
        vo.setCode(code.getCode());
        vo.setProductType(code.getProductType());

        // 产品描述
        ProductTypeEnum productType = ProductTypeEnum.getByValue(code.getProductType());
        vo.setProductDescription(productType != null ? productType.getDescription() : "未知产品");

        vo.setMaxUses(code.getMaxUses());
        vo.setUsedCount(code.getUsedCount());
        vo.setStatus(code.getStatus());

        // 状态描述
        RedemptionStatusEnum statusEnum = RedemptionStatusEnum.getByValue(code.getStatus());
        vo.setStatusDescription(statusEnum != null ? statusEnum.getDescription() : "未知状态");

        vo.setExpireTime(code.getExpireTime());
        vo.setDescription(code.getDescription());
        vo.setCreateTime(code.getCreateTime());
        return vo;
    }

    /**
     * 转换为兑换记录VO
     */
    private RedemptionRecordVO convertToRecordVO(RedemptionRecord record) {
        RedemptionRecordVO vo = new RedemptionRecordVO();
        vo.setId(record.getId());
        vo.setUserId(record.getUserId());
        vo.setCode(record.getCode());
        vo.setProductType(record.getProductType());

        // 产品描述
        ProductTypeEnum productType = ProductTypeEnum.getByValue(record.getProductType());
        vo.setProductDescription(productType != null ? productType.getDescription() : "未知产品");

        vo.setStatus(record.getStatus());

        // 状态描述
        RedemptionRecordStatusEnum statusEnum = RedemptionRecordStatusEnum.getByValue(record.getStatus());
        vo.setStatusDescription(statusEnum != null ? statusEnum.getDescription() : "未知状态");

        vo.setExpireTime(record.getExpireTime());
        vo.setDescription(record.getDescription());
        vo.setCreateTime(record.getCreateTime());
        return vo;
    }
}
