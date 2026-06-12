package com.pen.penhubbackend.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import com.pen.penhubbackend.model.entity.Template;

import java.util.List;

/**
 * 模板服务接口
 */
public interface TemplateService extends IService<Template> {

    /**
     * 分页查询模板
     *
     * @param category 分类（可为空）
     * @param platform 平台（可为空）
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    Page<Template> listTemplates(String category, String platform, long pageNum, long pageSize);

    /**
     * 获取模板详情
     *
     * @param id 模板ID
     * @return 模板实体
     */
    Template getTemplateById(Long id);
}
