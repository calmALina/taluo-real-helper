package com.taluo.taluorealhelper.ai.Guardrail;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailResult;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * AI塔罗师输入安全护栏
 * 核心规则：
 * 1. 禁止敏感/有害问题（生死、疾病、赌博、违法等）
 * 2. 禁止无意义/辱骂性输入
 * 3. 引导用户提出具体的占卜场景（爱情/事业/学业/人际等）
 * 4. 拒绝极端/有害结果的预测请求
 */
@Slf4j
public class SafeInputGuardrail implements InputGuardrail {

    // ########################### 核心配置 ###########################
    // 1. 敏感关键词列表（塔罗师需禁止的问题类型）
    private static final List<String> SENSITIVE_KEYWORDS = Arrays.asList(
            // 违法/有害
            "赌博", "作弊", "诈骗", "盗窃", "杀人", "吸毒", "嫖娼",
            // 极端场景
            "生死", "绝症", "自杀", "堕胎", "离婚", "家暴",
            // 无意义/辱骂
            "傻逼", "操", "尼玛", "去死", "垃圾", "废物",
            // 非塔罗范畴
            "算命", "改命", "破财", "升官", "中奖", "彩票", "股票"
    );

    // 2. 有效占卜场景关键词（引导用户提问）
    private static final List<String> VALID_SCENARIOS = Arrays.asList(
            "爱情", "事业", "学业", "人际", "财运", "健康", "成长", "选择", "未来", "关系"
    );

    // 3. 模糊问题正则（无具体指向的问题）
    private static final Pattern VAGUE_QUESTION_PATTERN = Pattern.compile(
            "^(塔罗牌|占卜|算一下|看看|怎么样|好不好|行不行)$",
            Pattern.CASE_INSENSITIVE
    );

    // ########################### 核心校验逻辑 ###########################
    @Override
    public InputGuardrailResult validate(UserMessage userMessage) {
        // 1. 获取用户输入文本（去除首尾空格）
        String userInput = userMessage.singleText().trim();
        log.info("AI塔罗师输入校验：{}", userInput);

        // 2. 空输入校验
        if (userInput.isEmpty()) {
            return failure("❌ 输入不能为空，请提出具体的塔罗占卜问题（如：我的爱情运势如何？）");
        }

        // 3. 敏感关键词校验（拒绝违规输入）
        if (containsSensitiveKeywords(userInput)) {
            return failure(
                    "❌ 你的问题涉及敏感/有害内容，不符合塔罗占卜的核心原则。\n" +
                            "塔罗师仅提供心灵指引，不解答违法、极端、有害的问题。\n" +
                            "建议提问方向：爱情/事业/学业/人际等成长类问题。"
            );
        }

        // 4. 模糊问题校验（引导用户补充具体问题）
        if (isVagueQuestion(userInput)) {
            return failure(
                    "❌ 你的问题过于模糊，请补充具体的占卜场景和问题。\n" +
                            "示例：\n" +
                            "- 爱情：这段关系是否值得继续？\n" +
                            "- 事业：新工作机会是否适合我？\n" +
                            "- 学业：备考能否取得理想成绩？"
            );
        }


        // 6. 所有校验通过
        return success();
    }

    // ########################### 辅助方法 ###########################
    /**
     * 检查输入是否包含敏感关键词
     */
    private boolean containsSensitiveKeywords(String input) {
        String lowerInput = input.toLowerCase();
        return SENSITIVE_KEYWORDS.stream()
                .anyMatch(keyword -> lowerInput.contains(keyword.toLowerCase()));
    }

    /**
     * 检查是否为模糊问题（无具体指向）
     */
    private boolean isVagueQuestion(String input) {
        return VAGUE_QUESTION_PATTERN.matcher(input).matches();
    }

    /**
     * 检查输入是否包含有效占卜场景
     */
    private boolean containsValidScenario(String input) {
        String lowerInput = input.toLowerCase();
        return VALID_SCENARIOS.stream()
                .anyMatch(scenario -> lowerInput.contains(scenario.toLowerCase()));
    }
}