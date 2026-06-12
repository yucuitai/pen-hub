package com.pen.penhubbackend.mapper;

import com.mybatisflex.core.BaseMapper;
import com.pen.penhubbackend.model.entity.Template;
import org.apache.ibatis.annotations.Mapper;

/**
 * 模板表 Mapper 接口
 */
@Mapper
public interface TemplateMapper extends BaseMapper<Template> {
}
