package com.taluo.taluorealhelper.ai.Tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
        import java.util.stream.Collectors;

/**
 * 塔罗中国网站爬虫Tool工具类
 * 用于LangChain4j AI Agent调用
 */
@Slf4j
@Component
public class TaluoKnowledgeTool {

    private static final String BASE_URL = "https://www.tarotchina.net";
    private static final int TIMEOUT = 10000;
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";

    @Autowired
    private TarotDataCache dataCache;

    /**
     * 搜索塔罗牌信息
     * @param cardName 塔罗牌名称，例如："愚者"、"魔术师"、"The Fool"
     * @return 塔罗牌的详细信息
     */
    @Tool(name = "searchTarotCard",value= "搜索并获取指定塔罗牌的详细信息，包括正位和逆位含义、关键词等")
    public String searchTarotCard(String cardName) {
        log.info("搜索塔罗牌: {}", cardName);

        try {
            // 先从缓存查找
            String cached = dataCache.getCard(cardName);
            if (cached != null) {
                return cached;
            }

            // 爬取网站数据
            Document doc = connectWithRetry(BASE_URL + "/tarot/" + normalizeCardName(cardName));

            if (doc == null) {
                return "未找到塔罗牌: " + cardName;
            }

            StringBuilder result = new StringBuilder();
            result.append("【").append(cardName).append("】\n\n");

            // 解析牌的基本信息
            String cardNumber = getTextSafe(doc, ".card-number");
            String cardType = getTextSafe(doc, ".card-type");

            if (!cardNumber.isEmpty()) {
                result.append("牌号: ").append(cardNumber).append("\n");
            }
            if (!cardType.isEmpty()) {
                result.append("类型: ").append(cardType).append("\n");
            }
            result.append("\n");

            // 正位含义
            String uprightMeaning = getTextSafe(doc, ".upright-meaning, .meaning-upright");
            if (!uprightMeaning.isEmpty()) {
                result.append("【正位含义】\n").append(uprightMeaning).append("\n\n");
            }

            // 逆位含义
            String reversedMeaning = getTextSafe(doc, ".reversed-meaning, .meaning-reversed");
            if (!reversedMeaning.isEmpty()) {
                result.append("【逆位含义】\n").append(reversedMeaning).append("\n\n");
            }

            // 关键词
            String keywords = getTextSafe(doc, ".keywords, .card-keywords");
            if (!keywords.isEmpty()) {
                result.append("【关键词】\n").append(keywords).append("\n\n");
            }

            // 详细描述
            String description = getTextSafe(doc, ".description, .card-description");
            if (!description.isEmpty()) {
                result.append("【详细描述】\n").append(description).append("\n");
            }

            String finalResult = result.toString();

            // 缓存结果
            dataCache.putCard(cardName, finalResult);

            return finalResult;

        } catch (Exception e) {
            log.error("搜索塔罗牌失败: {}", cardName, e);
            return "搜索塔罗牌时发生错误: " + e.getMessage();
        }
    }

    /**
     * 获取所有大阿尔卡纳牌列表
     * @return 大阿尔卡纳牌的列表信息
     */
    @Tool(name = "getMajorArcanaList",value= "获取塔罗牌大阿尔卡纳（22张主牌）的完整列表及简介")
    public String getMajorArcanaList() {
        log.info("获取大阿尔卡纳列表");

        try {
            String cached = dataCache.getMajorArcana();
            if (cached != null) {
                return cached;
            }

            Document doc = connectWithRetry(BASE_URL + "/major-arcana");

            if (doc == null) {
                return buildMajorArcanaDefault();
            }

            StringBuilder result = new StringBuilder("【塔罗牌大阿尔卡纳 - 22张主牌】\n\n");

            Elements cards = doc.select(".card-item, .arcana-card");

            if (cards.isEmpty()) {
                return buildMajorArcanaDefault();
            }

            for (Element card : cards) {
                String name = getTextSafe(card, ".card-name, h3, .title");
                String number = getTextSafe(card, ".card-number, .number");
                String brief = getTextSafe(card, ".card-brief, .description");

                if (!name.isEmpty()) {
                    result.append(number).append(" - ").append(name).append("\n");
                    if (!brief.isEmpty()) {
                        result.append("  ").append(brief).append("\n");
                    }
                    result.append("\n");
                }
            }

            String finalResult = result.toString();
            dataCache.putMajorArcana(finalResult);

            return finalResult;

        } catch (Exception e) {
            log.error("获取大阿尔卡纳列表失败", e);
            return buildMajorArcanaDefault();
        }
    }

    /**
     * 获取塔罗牌阵信息
     * @param spreadName 牌阵名称，例如："凯尔特十字"、"三张牌"
     * @return 牌阵的详细布局和使用方法
     */
    @Tool(name = "getTarotSpread",value= "获取指定塔罗牌阵的详细信息，包括布局方式、位置含义和使用场景")
    public String getTarotSpread(String spreadName) {
        log.info("获取塔罗牌阵: {}", spreadName);

        try {
            String cached = dataCache.getSpread(spreadName);
            if (cached != null) {
                return cached;
            }

            Document doc = connectWithRetry(BASE_URL + "/spread/" + normalizeSpreadName(spreadName));

            if (doc == null) {
                return "未找到牌阵: " + spreadName;
            }

            StringBuilder result = new StringBuilder();
            result.append("【").append(spreadName).append("牌阵】\n\n");

            // 牌阵简介
            String intro = getTextSafe(doc, ".spread-intro, .introduction");
            if (!intro.isEmpty()) {
                result.append(intro).append("\n\n");
            }

            // 牌数
            String cardCount = getTextSafe(doc, ".card-count");
            if (!cardCount.isEmpty()) {
                result.append("使用牌数: ").append(cardCount).append("\n\n");
            }

            // 布局方式
            String layout = getTextSafe(doc, ".spread-layout, .layout");
            if (!layout.isEmpty()) {
                result.append("【布局方式】\n").append(layout).append("\n\n");
            }

            // 位置含义
            Elements positions = doc.select(".position-item, .card-position");
            if (!positions.isEmpty()) {
                result.append("【位置含义】\n");
                for (Element pos : positions) {
                    String posNumber = getTextSafe(pos, ".position-number");
                    String posMeaning = getTextSafe(pos, ".position-meaning");
                    if (!posMeaning.isEmpty()) {
                        result.append(posNumber).append(". ").append(posMeaning).append("\n");
                    }
                }
                result.append("\n");
            }

            // 使用场景
            String usage = getTextSafe(doc, ".usage-scenario, .when-to-use");
            if (!usage.isEmpty()) {
                result.append("【适用场景】\n").append(usage).append("\n");
            }

            String finalResult = result.toString();
            dataCache.putSpread(spreadName, finalResult);

            return finalResult;

        } catch (Exception e) {
            log.error("获取塔罗牌阵失败: {}", spreadName, e);
            return "获取牌阵信息时发生错误: " + e.getMessage();
        }
    }

    /**
     * 获取塔罗占卜技巧和知识
     * @param topic 主题，例如："占卜方法"、"解牌技巧"、"洗牌方式"
     * @return 相关的塔罗知识内容
     */
    @Tool(name = "getTarotKnowledge",value= "获取塔罗占卜相关的知识和技巧，包括占卜方法、解牌技巧等")
    public String getTarotKnowledge(@P(value = "the topic you want search") String topic) {
        log.info("获取塔罗知识: {}", topic);

        try {
            String cached = dataCache.getKnowledge(topic);
            if (cached != null) {
                return cached;
            }

            // 搜索相关文章
            Document searchDoc = connectWithRetry(BASE_URL + "/search?q=" + topic);

            if (searchDoc == null) {
                return "未找到相关知识: " + topic;
            }

            StringBuilder result = new StringBuilder();
            result.append("【").append(topic).append(" - 相关知识】\n\n");

            Elements articles = searchDoc.select(".article-item, .knowledge-item");

            if (articles.isEmpty()) {
                return "未找到关于 \"" + topic + "\" 的相关知识";
            }

            // 获取前3篇文章内容
            int count = 0;
            for (Element article : articles) {
                if (count >= 3) break;

                String articleUrl = article.select("a").attr("href");
                if (!articleUrl.startsWith("http")) {
                    articleUrl = BASE_URL + articleUrl;
                }

                Document articleDoc = connectWithRetry(articleUrl);
                if (articleDoc != null) {
                    String title = getTextSafe(articleDoc, "h1, .article-title");
                    String content = getTextSafe(articleDoc, ".article-content, .content");

                    result.append("━━━━━━━━━━━━━━━━\n");
                    result.append("《").append(title).append("》\n\n");
                    result.append(content).append("\n\n");

                    count++;
                }

                // 延迟避免请求过快
                Thread.sleep(500);
            }

            String finalResult = result.toString();
            dataCache.putKnowledge(topic, finalResult);

            return finalResult;

        } catch (Exception e) {
            log.error("获取塔罗知识失败: {}", topic, e);
            return "获取知识时发生错误: " + e.getMessage();
        }
    }

    /**
     * 搜索塔罗牌关键词
     * @param keyword 关键词，例如："爱情"、"事业"、"财富"
     * @return 与关键词相关的塔罗牌列表
     */
    @Tool(name = "searchByKeyword",value= "根据关键词搜索相关的塔罗牌，例如搜索'爱情'会返回与爱情相关的牌")
    public String searchByKeyword(String keyword) {
        log.info("搜索关键词: {}", keyword);

        try {
            Document doc = connectWithRetry(BASE_URL + "/search?keyword=" + keyword);

            if (doc == null) {
                return "搜索失败";
            }

            StringBuilder result = new StringBuilder();
            result.append("【与 \"").append(keyword).append("\" 相关的塔罗牌】\n\n");

            Elements cards = doc.select(".search-result-item, .card-item");

            if (cards.isEmpty()) {
                return "未找到与 \"" + keyword + "\" 相关的塔罗牌";
            }

            for (Element card : cards) {
                String name = getTextSafe(card, ".card-name, .title");
                String brief = getTextSafe(card, ".brief, .description");

                if (!name.isEmpty()) {
                    result.append("• ").append(name).append("\n");
                    if (!brief.isEmpty()) {
                        result.append("  ").append(brief).append("\n");
                    }
                    result.append("\n");
                }
            }

            return result.toString();

        } catch (Exception e) {
            log.error("搜索关键词失败: {}", keyword, e);
            return "搜索时发生错误: " + e.getMessage();
        }
    }

    /**
     * 获取每日一牌推荐
     * @return 今日塔罗牌及其含义
     */
    @Tool(name = "getDailyCard",value= "获取今日推荐的塔罗牌，用于每日占卜参考")
    public String getDailyCard() {
        log.info("获取每日一牌");

        try {
            // 随机选择一张牌
            String[] majorArcana = {
                    "愚者", "魔术师", "女祭司", "皇后", "皇帝", "教皇",
                    "恋人", "战车", "力量", "隐者", "命运之轮", "正义",
                    "倒吊人", "死神", "节制", "恶魔", "塔", "星星",
                    "月亮", "太阳", "审判", "世界"
            };

            Random random = new Random();
            String cardName = majorArcana[random.nextInt(majorArcana.length)];

            // 获取该牌详情
            return "【今日一牌】\n\n" + searchTarotCard(cardName);

        } catch (Exception e) {
            log.error("获取每日一牌失败", e);
            return "获取每日一牌时发生错误";
        }
    }

    // ========== 辅助方法 ==========

    /**
     * 带重试的连接方法
     */
    private Document connectWithRetry(String url) {
        int maxRetries = 3;
        for (int i = 0; i < maxRetries; i++) {
            try {
                return Jsoup.connect(url)
                        .userAgent(USER_AGENT)
                        .timeout(TIMEOUT)
                        .get();
            } catch (IOException e) {
                log.warn("连接失败 (尝试 {}/{}): {}", i + 1, maxRetries, url);
                if (i == maxRetries - 1) {
                    log.error("连接最终失败: {}", url, e);
                    return null;
                }
                try {
                    Thread.sleep(1000 * (i + 1));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        return null;
    }

    /**
     * 安全获取文本内容
     */
    private String getTextSafe(Element element, String cssQuery) {
        Elements elements = element.select(cssQuery);
        return elements.isEmpty() ? "" : elements.first().text().trim();
    }

    /**
     * 标准化牌名（用于URL）
     */
    private String normalizeCardName(String cardName) {
        // 中文牌名映射到URL路径
        Map<String, String> nameMap = new HashMap<>();
        nameMap.put("愚者", "fool");
        nameMap.put("魔术师", "magician");
        nameMap.put("女祭司", "high-priestess");
        // ... 添加更多映射

        return nameMap.getOrDefault(cardName.toLowerCase(),
                cardName.toLowerCase().replace(" ", "-"));
    }

    /**
     * 标准化牌阵名称
     */
    private String normalizeSpreadName(String spreadName) {
        Map<String, String> nameMap = new HashMap<>();
        nameMap.put("凯尔特十字", "celtic-cross");
        nameMap.put("三张牌", "three-card");
        nameMap.put("关系牌阵", "relationship");

        return nameMap.getOrDefault(spreadName,
                spreadName.toLowerCase().replace(" ", "-"));
    }

    /**
     * 构建默认大阿尔卡纳列表
     */
    private String buildMajorArcanaDefault() {
        return """
                【塔罗牌大阿尔卡纳 - 22张主牌】
                
                0 - 愚者 (The Fool) - 新开始、冒险、纯真
                1 - 魔术师 (The Magician) - 创造力、行动、显化
                2 - 女祭司 (The High Priestess) - 直觉、神秘、内在智慧
                3 - 皇后 (The Empress) - 丰饶、母性、创造
                4 - 皇帝 (The Emperor) - 权威、结构、父性
                5 - 教皇 (The Hierophant) - 传统、教育、信仰
                6 - 恋人 (The Lovers) - 爱情、选择、和谐
                7 - 战车 (The Chariot) - 意志力、胜利、前进
                8 - 力量 (Strength) - 勇气、耐心、内在力量
                9 - 隐者 (The Hermit) - 内省、智慧、孤独
                10 - 命运之轮 (Wheel of Fortune) - 命运、循环、机会
                11 - 正义 (Justice) - 公平、真相、因果
                12 - 倒吊人 (The Hanged Man) - 牺牲、新视角、暂停
                13 - 死神 (Death) - 转变、结束、重生
                14 - 节制 (Temperance) - 平衡、调和、耐心
                15 - 恶魔 (The Devil) - 束缚、物欲、阴影
                16 - 塔 (The Tower) - 突变、崩溃、启示
                17 - 星星 (The Star) - 希望、灵感、疗愈
                18 - 月亮 (The Moon) - 幻象、潜意识、恐惧
                19 - 太阳 (The Sun) - 成功、喜悦、生命力
                20 - 审判 (Judgement) - 觉醒、救赎、召唤
                21 - 世界 (The World) - 完成、成就、整合
                """;
    }
}

/**
 * 数据缓存服务
 */
@Component
class TarotDataCache {

    private final Map<String, String> cardCache = new HashMap<>();
    private final Map<String, String> spreadCache = new HashMap<>();
    private final Map<String, String> knowledgeCache = new HashMap<>();
    private String majorArcanaCache;

    @Cacheable(value = "tarotCard", key = "#cardName")
    public String getCard(String cardName) {
        return cardCache.get(cardName);
    }

    public void putCard(String cardName, String data) {
        cardCache.put(cardName, data);
    }

    @Cacheable(value = "tarotSpread", key = "#spreadName")
    public String getSpread(String spreadName) {
        return spreadCache.get(spreadName);
    }

    public void putSpread(String spreadName, String data) {
        spreadCache.put(spreadName, data);
    }

    @Cacheable(value = "tarotKnowledge", key = "#topic")
    public String getKnowledge(String topic) {
        return knowledgeCache.get(topic);
    }

    public void putKnowledge(String topic, String data) {
        knowledgeCache.put(topic, data);
    }

    public String getMajorArcana() {
        return majorArcanaCache;
    }

    public void putMajorArcana(String data) {
        majorArcanaCache = data;
    }

    public void clear() {
        cardCache.clear();
        spreadCache.clear();
        knowledgeCache.clear();
        majorArcanaCache = null;
    }
}