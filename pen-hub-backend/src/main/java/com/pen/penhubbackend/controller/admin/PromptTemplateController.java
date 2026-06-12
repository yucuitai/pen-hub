package com.pen.penhubbackend.controller.admin;

import com.mybatisflex.core.paginate.Page;
import com.pen.penhubbackend.common.BaseResponse;
import com.pen.penhubbackend.common.ResultUtils;
import com.pen.penhubbackend.exception.ErrorCode;
import com.pen.penhubbackend.exception.ThrowUtils;
import com.pen.penhubbackend.model.entity.PromptTemplate;
import com.pen.penhubbackend.model.entity.User;
import com.pen.penhubbackend.service.PromptTemplateService;
import com.pen.penhubbackend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/prompt")
@Slf4j
@Tag(name = "Prompt管理", description = "Prompt模板管理接口（管理员）")
public class PromptTemplateController {

    @Resource
    private PromptTemplateService promptTemplateService;

    @Resource
    private UserService userService;

    @GetMapping("/list")
    @Operation(summary = "获取Prompt模板列表")
    public BaseResponse<Page<PromptTemplate>> list(
            @RequestParam(required = false) String agentName,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "20") long pageSize,
            HttpServletRequest request) {
        checkAdmin(request);
        return ResultUtils.success(promptTemplateService.listTemplates(agentName, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取Prompt模板详情")
    public BaseResponse<PromptTemplate> get(@PathVariable Long id, HttpServletRequest request) {
        checkAdmin(request);
        return ResultUtils.success(promptTemplateService.getTemplateById(id));
    }

    @PostMapping("/create")
    @Operation(summary = "创建Prompt模板")
    public BaseResponse<Boolean> create(@RequestBody PromptTemplate template, HttpServletRequest request) {
        checkAdmin(request);
        ThrowUtils.throwIf(template.getName() == null || template.getAgentName() == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(promptTemplateService.createTemplate(template));
    }

    @PutMapping("/update")
    @Operation(summary = "更新Prompt模板")
    public BaseResponse<Boolean> update(@RequestBody PromptTemplate template, HttpServletRequest request) {
        checkAdmin(request);
        ThrowUtils.throwIf(template.getId() == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(promptTemplateService.updateTemplate(template));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除Prompt模板")
    public BaseResponse<Boolean> delete(@PathVariable Long id, HttpServletRequest request) {
        checkAdmin(request);
        return ResultUtils.success(promptTemplateService.deleteTemplate(id));
    }

    private void checkAdmin(HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        ThrowUtils.throwIf(!"admin".equals(user.getUserRole()), ErrorCode.NO_AUTH_ERROR, "仅管理员可操作");
    }
}
