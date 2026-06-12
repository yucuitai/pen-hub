package com.pen.penhubbackend.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import com.pen.penhubbackend.model.entity.PromptTemplate;

public interface PromptTemplateService extends IService<PromptTemplate> {
    Page<PromptTemplate> listTemplates(String agentName, long pageNum, long pageSize);
    PromptTemplate getTemplateById(Long id);
    boolean createTemplate(PromptTemplate template);
    boolean updateTemplate(PromptTemplate template);
    boolean deleteTemplate(Long id);
}
