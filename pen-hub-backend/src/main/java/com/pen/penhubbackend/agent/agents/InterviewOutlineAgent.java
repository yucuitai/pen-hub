package com.pen.penhubbackend.agent.agents;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.pen.penhubbackend.constant.PromptConstant;
import com.pen.penhubbackend.model.dto.script.InterviewState;
import com.pen.penhubbackend.utils.GsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 访谈提纲 Agent
 * 生成访谈提纲和段落规划
 *
 * @author pen-hub
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class InterviewOutlineAgent implements NodeAction {

    private final DashScopeChatModel chatModel;

    public static final String INPUT_TOPIC = "topic";
    public static final String INPUT_PLATFORM = "platform";
    public static final String INPUT_DURATION = "duration";
    public static final String INPUT_STYLE = "style";
    public static final String OUTPUT_OUTLINE = "outline";

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String topic = state.value(INPUT_TOPIC)
                .map(Object::toString)
                .orElseThrow(() -> new IllegalArgumentException("缺少选题参数"));

        String platform = state.value(INPUT_PLATFORM)
                .map(Object::toString)
                .orElse("douyin");

        String duration = state.value(INPUT_DURATION)
                .map(Object::toString)
                .orElse("30min");

        String style = state.value(INPUT_STYLE)
                .map(Object::toString)
                .orElse("formal");

        log.info("InterviewOutlineAgent 开始执行: topic={}, platform={}, duration={}, style={}", topic, platform, duration, style);

        String prompt = PromptConstant.INTERVIEW_OUTLINE_PROMPT
                .replace("{topic}", topic)
                .replace("{platform}", platform)
                .replace("{duration}", duration)
                .replace("{style}", style);

        ChatResponse response = chatModel.call(new Prompt(new UserMessage(prompt)));
        String content = response.getResult().getOutput().getText();

        InterviewState.InterviewOutlineResult outline = GsonUtils.fromJson(content, InterviewState.InterviewOutlineResult.class);

        log.info("InterviewOutlineAgent 执行完成: 段落数={}", outline.getSegments() != null ? outline.getSegments().size() : 0);

        return Map.of(OUTPUT_OUTLINE, outline);
    }
}
