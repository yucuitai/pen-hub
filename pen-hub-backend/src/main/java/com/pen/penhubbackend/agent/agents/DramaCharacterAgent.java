package com.pen.penhubbackend.agent.agents;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.pen.penhubbackend.constant.PromptConstant;
import com.pen.penhubbackend.model.dto.drama.DramaState;
import com.pen.penhubbackend.utils.GsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 剧本角色设定 Agent
 * 根据选题、类型和风格生成角色设定
 *
 * @author pen-hub
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DramaCharacterAgent implements NodeAction {

    private final DashScopeChatModel chatModel;

    public static final String INPUT_TOPIC = "topic";
    public static final String INPUT_GENRE = "genre";
    public static final String INPUT_STYLE = "style";
    public static final String OUTPUT_CHARACTER_RESULT = "characterResult";

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String topic = state.value(INPUT_TOPIC)
                .map(Object::toString)
                .orElseThrow(() -> new IllegalArgumentException("缺少选题参数"));

        String genre = state.value(INPUT_GENRE)
                .map(Object::toString)
                .orElse("drama");

        String style = state.value(INPUT_STYLE)
                .map(Object::toString)
                .orElse("natural");

        log.info("DramaCharacterAgent 开始执行: topic={}, genre={}, style={}", topic, genre, style);

        String prompt = PromptConstant.DRAMA_CHARACTER_PROMPT
                .replace("{topic}", topic)
                .replace("{genre}", genre)
                .replace("{style}", style);

        ChatResponse response = chatModel.call(new Prompt(new UserMessage(prompt)));
        String content = response.getResult().getOutput().getText();

        DramaState.DramaCharacterResult characterResult = GsonUtils.fromJson(content, DramaState.DramaCharacterResult.class);

        log.info("DramaCharacterAgent 执行完成: 角色数={}",
                characterResult.getCharacters() != null ? characterResult.getCharacters().size() : 0);

        return Map.of(OUTPUT_CHARACTER_RESULT, characterResult);
    }
}
