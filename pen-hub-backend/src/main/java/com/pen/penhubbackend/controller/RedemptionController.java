package com.pen.penhubbackend.controller;

import com.pen.penhubbackend.annotation.AuthCheck;
import com.pen.penhubbackend.common.BaseResponse;
import com.pen.penhubbackend.common.ResultUtils;
import com.pen.penhubbackend.constant.UserConstant;
import com.pen.penhubbackend.model.dto.redemption.RedemptionCodeCreateRequest;
import com.pen.penhubbackend.model.dto.redemption.RedemptionRequest;
import com.pen.penhubbackend.model.entity.User;
import com.pen.penhubbackend.model.vo.RedemptionCodeVO;
import com.pen.penhubbackend.model.vo.RedemptionRecordVO;
import com.pen.penhubbackend.service.RedemptionService;
import com.pen.penhubbackend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 兑换码控制器
 */
@RestController
@RequestMapping("/redemption")
@Slf4j
@Tag(name = "RedemptionController", description = "兑换码接口")
public class RedemptionController {

    @Resource
    private RedemptionService redemptionService;

    @Resource
    private UserService userService;

    /**
     * 创建兑换码
     */
    @PostMapping("/code")
    @Operation(summary = "创建兑换码")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<List<RedemptionCodeVO>> createRedemptionCode(
            @RequestBody RedemptionCodeCreateRequest request) {
        List<RedemptionCodeVO> codes = redemptionService.createRedemptionCodes(request);
        return ResultUtils.success(codes);
    }

    /**
     * 兑换会员
     */
    @PostMapping("/redeem")
    @Operation(summary = "兑换会员")
    public BaseResponse<RedemptionRecordVO> redeemCode(
            @RequestBody RedemptionRequest request,
            HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        RedemptionRecordVO record = redemptionService.redeemCode(loginUser.getId(), request);
        return ResultUtils.success(record);
    }

    /**
     * 查询我的兑换记录
     */
    @GetMapping("/records")
    @Operation(summary = "查询我的兑换记录")
    public BaseResponse<List<RedemptionRecordVO>> getMyRedemptionRecords(HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        List<RedemptionRecordVO> records = redemptionService.getUserRedemptionRecords(loginUser.getId());
        return ResultUtils.success(records);
    }

    /**
     * 查询所有兑换记录（管理员）
     */
    @GetMapping("/records/all")
    @Operation(summary = "查询所有兑换记录")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<List<RedemptionRecordVO>> getAllRedemptionRecords() {
        List<RedemptionRecordVO> records = redemptionService.getAllRedemptionRecords();
        return ResultUtils.success(records);
    }

    /**
     * 查询所有兑换码（管理员）
     */
    @GetMapping("/codes")
    @Operation(summary = "查询所有兑换码")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<List<RedemptionCodeVO>> getAllRedemptionCodes() {
        List<RedemptionCodeVO> codes = redemptionService.getAllRedemptionCodes();
        return ResultUtils.success(codes);
    }
}
