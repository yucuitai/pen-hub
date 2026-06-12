package com.pen.penhubbackend.mapper;

import com.mybatisflex.core.BaseMapper;
import com.pen.penhubbackend.model.entity.ArticleFeedback;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文章反馈 Mapper 接口
 *
 * @author pen-hub
 */
@Mapper
public interface ArticleFeedbackMapper extends BaseMapper<ArticleFeedback> {
}
