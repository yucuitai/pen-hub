package com.pen.penhubbackend.service.impl;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.pen.penhubbackend.exception.ErrorCode;
import com.pen.penhubbackend.exception.ThrowUtils;
import com.pen.penhubbackend.mapper.PromptTemplateMapper;
import com.pen.penhubbackend.model.entity.PromptTemplate;
import com.pen.penhubbackend.service.PromptTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
public class PromptTemplateServiceImpl extends ServiceImpl<PromptTemplateMapper, PromptTemplate> implements PromptTemplateService {

    @Override
    public Page<PromptTemplate> listTemplates(String agentName, long pageNum, long pageSize) {
        QueryWrapper query = QueryWrapper.create().eq("is_delete", 0);
        if (agentName != null && !agentName.isEmpty()) {
            query.eq("agent_name", agentName);
        }
        query.orderBy("id", true);
        return this.page(new Page<>(pageNum, pageSize), query);
    }

    @Override
    public PromptTemplate getTemplateById(Long id) {
        PromptTemplate template = this.getById(id);
        ThrowUtils.throwIf(template == null, ErrorCode.NOT_FOUND_ERROR, "Prompt模板不存在");
        return template;
    }

    @Override
    public boolean createTemplate(PromptTemplate template) {
        template.setVersion(1);
        template.setStatus(1);
        template.setCreateTime(LocalDateTime.now());
        template.setUpdateTime(LocalDateTime.now());
        return this.save(template);
    }

    @Override
    public boolean updateTemplate(PromptTemplate template) {
        template.setUpdateTime(LocalDateTime.now());
        return this.updateById(template);
    }

    @Override
    public boolean deleteTemplate(Long id) {
        return this.removeById(id);
    }
}
