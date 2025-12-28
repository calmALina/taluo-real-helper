package com.taluo.taluorealhelper.ai;

import com.taluo.taluorealhelper.ai.Guardrail.SafeInputGuardrail;
import com.taluo.taluorealhelper.ai.Tool.TaluoKnowledgeTool;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
public class AiTaluoServiceFactory {

    @Resource
    private ChatModel qwenChatModel;

    @Resource
    private ContentRetriever contentRetriever;

    @Resource
    private McpToolProvider mcpToolProvider;

    @Resource
    private StreamingChatModel streamingChatModel;

    @Bean
    public AiTaluoService getAiTaluoService(){
        //会话记忆
        MessageWindowChatMemory messageWindowChatMemory = MessageWindowChatMemory.withMaxMessages(10);
        //构造 AI service
        AiTaluoService built = AiServices.builder(AiTaluoService.class)
                .chatModel(qwenChatModel)
                .streamingChatModel(streamingChatModel)//流式输出
                .chatMemory(messageWindowChatMemory)//会话记忆
                .chatMemoryProvider(memoryId ->MessageWindowChatMemory.withMaxMessages(10))//每个会话独立存储
                .contentRetriever(contentRetriever)//Rag检索
                .tools(new TaluoKnowledgeTool())//工具调用
                .toolProvider(mcpToolProvider)//Mcp工具调用
                .inputGuardrails(Arrays.asList(new InputGuardrail[]{new SafeInputGuardrail()}))
                .build();
        return built;
    }
}
