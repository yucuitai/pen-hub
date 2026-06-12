package com.pen.penhubbackend.controller;

import com.mybatisflex.core.paginate.Page;
import com.pen.penhubbackend.common.BaseResponse;
import com.pen.penhubbackend.common.ResultUtils;
import com.pen.penhubbackend.model.entity.Template;
import com.pen.penhubbackend.service.TemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 模板接口
 */
@RestController
@RequestMapping("/template")
@Slf4j
public class TemplateController {

    @Resource
    private TemplateService templateService;

    /**
     * 分页查询模板列表
     */
    @GetMapping("/list")
    @Operation(summary = "获取模板列表")
    public BaseResponse<Page<Template>> listTemplates(
            @Parameter(description = "分类") @RequestParam(required = false) String category,
            @Parameter(description = "平台") @RequestParam(required = false) String platform,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") long pageSize) {
        Page<Template> page = templateService.listTemplates(category, platform, pageNum, pageSize);
        return ResultUtils.success(page);
    }

    /**
     * 获取模板详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取模板详情")
    public BaseResponse<Template> getTemplate(@PathVariable Long id) {
        Template template = templateService.getTemplateById(id);
        return ResultUtils.success(template);
    }
}
