package com.pen.penhubbackend.agent;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;

/**
 * 智能体1：标题生成
 * 根据选题生成 3-5 个爆款标题方案
 */
@Data
@Slf4j
@RequiredArgsConstructor
public class TitleGeneratorAgent {


    //DashScope系列模型
    private final DashScopeChatModel dashScopeChatModel;
    //OpenAI系列模型
    private final ChatModel openAiChatModel;





}
