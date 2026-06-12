package com.pen.penhubbackend.service.impl;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.pen.penhubbackend.exception.ErrorCode;
import com.pen.penhubbackend.exception.ThrowUtils;
import com.pen.penhubbackend.mapper.TemplateMapper;
import com.pen.penhubbackend.model.entity.Template;
import com.pen.penhubbackend.service.TemplateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 模板服务实现类
 */
@Service
@Slf4j
public class TemplateServiceImpl extends ServiceImpl<TemplateMapper, Template> implements TemplateService {

    @Override
    public Page<Template> listTemplates(String category, String platform, long pageNum, long pageSize) {
        QueryWrapper query = QueryWrapper.create()
                .eq("status", 1)
                .eq("is_delete", 0);
        if (category != null && !category.isEmpty()) {
            query.eq("category", category);
        }
        if (platform != null && !platform.isEmpty()) {
            query.eq("platform", platform);
        }
        query.orderBy("sort_order", true);
        return this.page(new Page<>(pageNum, pageSize), query);
    }

    @Override
    public Template getTemplateById(Long id) {
        Template template = this.getById(id);
        ThrowUtils.throwIf(template == null, ErrorCode.NOT_FOUND_ERROR, "模板不存在");
        return template;
    }
}
