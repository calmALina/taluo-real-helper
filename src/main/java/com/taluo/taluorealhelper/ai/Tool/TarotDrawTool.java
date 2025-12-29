package com.taluo.taluorealhelper.ai.Tool;

import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

@Component
public class TarotDrawTool {

    @Tool("当用户想要抽塔罗牌、占卜、抽牌时调用此工具。返回特殊格式的消息,触发前端显示抽牌界面")
    public String triggerTarotDraw(String userQuestion) {
        // 返回特殊标记,前端识别后显示抽牌界面
        return "[TAROT_DRAW_START]" +
                "{\"question\":\"" + userQuestion + "\"," +
                "\"message\":\"请从下面选择" + getDrawCount(userQuestion) + "张塔罗牌\"}" +
                "[TAROT_DRAW_END]";
    }

    @Tool("分析用户抽取的塔罗牌结果")
    public String analyzeTarotCards(String cardsJson, String userQuestion) {
        // 这里只是返回确认信息,实际分析由AI完成
        return "收到抽牌结果: " + cardsJson + "。问题: " + userQuestion + "。现在开始为你解读...";
    }

    private int getDrawCount(String question) {
        // 根据问题类型决定抽几张牌,这里简化为固定3张
        if (question.contains("单张") || question.contains("一张")) {
            return 1;
        } else if (question.contains("五张") || question.contains("凯尔特十字")) {
            return 5;
        }
        return 3; // 默认三张牌阵
    }
}