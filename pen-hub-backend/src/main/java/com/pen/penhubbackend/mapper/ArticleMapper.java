package com.pen.penhubbackend.mapper;

import com.mybatisflex.core.BaseMapper;
import com.pen.penhubbackend.model.entity.Article;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文章表 Mapper 接口
 */
@Mapper
public interface ArticleMapper extends BaseMapper<Article> {
}
