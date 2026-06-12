package com.pen.penhubbackend.agent.agents;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.pen.penhubbackend.constant.PromptConstant;
import com.pen.penhubbackend.model.dto.script.ScriptState;
import com.pen.penhubbackend.utils.GsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 脚本 Hook Agent
 * 生成短视频开头 Hook（前 3 秒）
 *
 * @author pen-hub
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ScriptHookAgent implements NodeAction {

    private final DashScopeChatModel chatModel;

    public static final String INPUT_TOPIC = "topic";
    public static final String INPUT_PLATFORM = "platform";
    public static final String INPUT_DURATION = "duration";
    public static final String INPUT_STYLE = "style";
    public static final String OUTPUT_HOOK_RESULT = "hookResult";

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
                .orElse("30s");

        String style = state.value(INPUT_STYLE)
                .map(Object::toString)
                .orElse("funny");

        log.info("ScriptHookAgent 开始执行: topic={}, platform={}, duration={}, style={}", topic, platform, duration, style);

        String prompt = PromptConstant.SCRIPT_HOOK_PROMPT
                .replace("{topic}", topic)
                .replace("{platform}", platform)
                .replace("{duration}", duration)
                .replace("{style}", style);

        ChatResponse response = chatModel.call(new Prompt(new UserMessage(prompt)));
        String content = response.getResult().getOutput().getText();

        ScriptState.HookResult hookResult = GsonUtils.fromJson(content, ScriptState.HookResult.class);

        log.info("ScriptHookAgent 执行完成: hookType={}, hookText长度={}",
                hookResult.getHookType(), hookResult.getHookText() != null ? hookResult.getHookText().length() : 0);

        return Map.of(OUTPUT_HOOK_RESULT, hookResult);
    }
}
