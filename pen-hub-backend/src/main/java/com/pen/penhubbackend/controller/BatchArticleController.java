package com.pen.penhubbackend.controller;

import com.pen.penhubbackend.common.BaseResponse;
import com.pen.penhubbackend.common.ResultUtils;
import com.pen.penhubbackend.exception.ErrorCode;
import com.pen.penhubbackend.exception.ThrowUtils;
import com.pen.penhubbackend.model.entity.User;
import com.pen.penhubbackend.service.ArticleService;
import com.pen.penhubbackend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/batch")
@Slf4j
@Tag(name = "批量创作", description = "批量文章创作接口")
public class BatchArticleController {

    @Resource
    private ArticleService articleService;

    @Resource
    private UserService userService;

    @PostMapping("/create")
    @Operation(summary = "批量创建文章任务")
    public BaseResponse<List<String>> batchCreate(@RequestBody Map<String, Object> request,
                                                    HttpServletRequest httpServletRequest) {
        List<String> topics = (List<String>) request.get("topics");
        String style = (String) request.get("style");
        String contentType = (String) request.get("contentType");

        ThrowUtils.throwIf(topics == null || topics.isEmpty(), ErrorCode.PARAMS_ERROR, "选题列表不能为空");
        ThrowUtils.throwIf(topics.size() > 10, ErrorCode.PARAMS_ERROR, "单次最多批量创建10篇");

        User loginUser = userService.getLoginUser(httpServletRequest);
        // VIP 旗舰版限制
        ThrowUtils.throwIf(loginUser.getVipLevel() == null || loginUser.getVipLevel() < 3,
                ErrorCode.NO_AUTH_ERROR, "批量创作仅限旗舰版用户");

        List<String> taskIds = new ArrayList<>();
        for (String topic : topics) {
            if (topic == null || topic.trim().isEmpty()) continue;
            String taskId = articleService.createArticleTaskWithQuotaCheck(topic.trim(), style, null, loginUser);
            taskIds.add(taskId);
        }

        log.info("批量创建文章任务完成, userId={}, count={}", loginUser.getId(), taskIds.size());
        return ResultUtils.success(taskIds);
    }
}
