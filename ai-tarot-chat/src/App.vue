<template>
  <div class="app-container">
    <!-- 左侧会话列表 -->
    <div :class="['sidebar', { 'sidebar-open': isSidebarOpen }]">
      <!-- 侧边栏头部 -->
      <div class="sidebar-header">
        <div class="logo-section">
          <svg class="sparkles-icon" xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M9.937 15.5A2 2 0 0 0 8.5 14.063l-6.135-1.582a.5.5 0 0 1 0-.962L8.5 9.936A2 2 0 0 0 9.937 8.5l1.582-6.135a.5.5 0 0 1 .963 0L14.063 8.5A2 2 0 0 0 15.5 9.937l6.135 1.581a.5.5 0 0 1 0 .964L15.5 14.063a2 2 0 0 0-1.437 1.437l-1.582 6.135a.5.5 0 0 1-.963 0z"/>
          </svg>
          <span>AI塔罗师</span>
        </div>
        <button @click="createNewSession" class="new-chat-btn" title="新建会话">
          <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 5v14"/>
            <path d="M5 12h14"/>
          </svg>
        </button>
      </div>

      <!-- 会话列表 -->
      <div class="sessions-list">
        <div
            v-for="session in sessions"
            :key="session.id"
            :class="['session-item', { 'active': session.id === currentSessionId }]"
            @click="switchSession(session.id)"
        >
          <div class="session-info">
            <div class="session-title">{{ session.title }}</div>
            <div class="session-time">{{ session.lastMessageTime }}</div>
          </div>
          <button
              @click.stop="deleteSession(session.id)"
              class="delete-btn"
              title="删除会话"
              v-if="sessions.length > 1"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M3 6h18"/>
              <path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6"/>
              <path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"/>
            </svg>
          </button>
        </div>
      </div>
    </div>

    <!-- 主聊天区域 -->
    <div class="main-content">
      <!-- 移动端菜单按钮 -->
      <button @click="toggleSidebar" class="mobile-menu-btn">
        <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <line x1="3" y1="12" x2="21" y2="12"/>
          <line x1="3" y1="6" x2="21" y2="6"/>
          <line x1="3" y1="18" x2="21" y2="18"/>
        </svg>
      </button>

      <!-- Header -->
      <div class="header">
        <div class="header-content">
          <h1>{{ currentSession?.title || 'AI塔罗师' }}</h1>
          <p class="session-id">会话ID: {{ currentSession?.memoryId }}</p>
        </div>
      </div>

      <!-- Messages Area -->
      <div class="messages-area" ref="messagesContainer">
        <div class="messages-wrapper">
          <div
              v-for="msg in currentSession?.messages || []"
              :key="msg.id"
              :class="['message-row', msg.type === 'user' ? 'message-right' : 'message-left']"
          >
            <div :class="['message-bubble', msg.type]">
              <div class="message-content">{{ msg.content }}</div>
              <div class="message-time">{{ msg.timestamp }}</div>
            </div>
          </div>

          <!-- Loading indicator -->
          <div v-if="isStreaming && currentSession?.messages.length > 0 && currentSession.messages[currentSession.messages.length - 1].type === 'ai'" class="message-row message-left">
            <div class="message-bubble ai loading">
              <div class="loading-dots">
                <span></span>
                <span></span>
                <span></span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Input Area -->
      <div class="input-area">
        <div class="input-wrapper">
          <input
              v-model="inputValue"
              @keypress.enter="handleSendMessage"
              :disabled="isStreaming"
              type="text"
              placeholder="输入你的问题..."
              class="message-input"
          />
          <button
              @click="handleSendMessage"
              :disabled="!inputValue.trim() || isStreaming"
              class="send-button"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="m22 2-7 20-4-9-9-4Z"/>
              <path d="M22 2 11 13"/>
            </svg>
            发送
          </button>
        </div>
      </div>
    </div>

    <!-- 抽牌界面 -->
    <div v-if="showTarotDraw" class="tarot-draw-overlay" @click.self="closeTarotDraw">
      <div class="tarot-draw-modal">
        <div class="tarot-header">
          <h2>🔮 抽取塔罗牌</h2>
          <p class="tarot-message">{{ tarotDrawConfig?.message || '请选择塔罗牌' }}</p>
          <p class="tarot-progress">已选择: {{ selectedCards.length }} / {{ tarotDrawConfig?.count || 3 }}</p>
        </div>

        <div class="tarot-cards-grid">
          <button
              v-for="card in shuffledCards"
              :key="card.id"
              @click="handleCardSelect(card)"
              :disabled="isCardSelected(card) || selectedCards.length >= (tarotDrawConfig?.count || 3)"
              :class="['tarot-card', { 'selected': isCardSelected(card) }]"
          >
            {{ isCardSelected(card) ? '✓' : '?' }}
          </button>
        </div>

        <div v-if="selectedCards.length > 0" class="selected-cards-info">
          <h3>已选择的牌:</h3>
          <div class="selected-cards-list">
            <div v-for="(card, idx) in selectedCards" :key="card.id" class="selected-card-item">
              {{ idx + 1 }}. {{ card.name }} ({{ card.reversed ? '逆位' : '正位' }})
            </div>
          </div>
        </div>

        <div class="tarot-actions">
          <button @click="closeTarotDraw" class="tarot-btn tarot-btn-cancel">
            取消
          </button>
          <button
              v-if="selectedCards.length === (tarotDrawConfig?.count || 3)"
              @click="confirmTarotDraw"
              class="tarot-btn tarot-btn-confirm"
          >
            确认抽牌
          </button>
        </div>
      </div>
    </div>

    <!-- 遮罩层 (移动端) -->
    <div
        v-if="isSidebarOpen"
        class="overlay"
        @click="toggleSidebar"
    ></div>
  </div>
</template>

<script>
import { ref, computed, nextTick, onMounted, watch } from 'vue';

// 78张塔罗牌数据
const TAROT_CARDS = [
  // 大阿尔卡那 (22张)
  { id: 0, name: '愚者', nameEn: 'The Fool', type: 'major' },
  { id: 1, name: '魔术师', nameEn: 'The Magician', type: 'major' },
  { id: 2, name: '女祭司', nameEn: 'The High Priestess', type: 'major' },
  { id: 3, name: '皇后', nameEn: 'The Empress', type: 'major' },
  { id: 4, name: '皇帝', nameEn: 'The Emperor', type: 'major' },
  { id: 5, name: '教皇', nameEn: 'The Hierophant', type: 'major' },
  { id: 6, name: '恋人', nameEn: 'The Lovers', type: 'major' },
  { id: 7, name: '战车', nameEn: 'The Chariot', type: 'major' },
  { id: 8, name: '力量', nameEn: 'Strength', type: 'major' },
  { id: 9, name: '隐士', nameEn: 'The Hermit', type: 'major' },
  { id: 10, name: '命运之轮', nameEn: 'Wheel of Fortune', type: 'major' },
  { id: 11, name: '正义', nameEn: 'Justice', type: 'major' },
  { id: 12, name: '倒吊人', nameEn: 'The Hanged Man', type: 'major' },
  { id: 13, name: '死神', nameEn: 'Death', type: 'major' },
  { id: 14, name: '节制', nameEn: 'Temperance', type: 'major' },
  { id: 15, name: '恶魔', nameEn: 'The Devil', type: 'major' },
  { id: 16, name: '高塔', nameEn: 'The Tower', type: 'major' },
  { id: 17, name: '星星', nameEn: 'The Star', type: 'major' },
  { id: 18, name: '月亮', nameEn: 'The Moon', type: 'major' },
  { id: 19, name: '太阳', nameEn: 'The Sun', type: 'major' },
  { id: 20, name: '审判', nameEn: 'Judgement', type: 'major' },
  { id: 21, name: '世界', nameEn: 'The World', type: 'major' },
  // 小阿尔卡那 - 权杖 (14张)
  { id: 22, name: '权杖ACE', nameEn: 'ACE of Wands', type: 'minor' },
  { id: 23, name: '权杖2', nameEn: '2 of Wands', type: 'minor' },
  { id: 24, name: '权杖3', nameEn: '3 of Wands', type: 'minor' },
  { id: 25, name: '权杖4', nameEn: '4 of Wands', type: 'minor' },
  { id: 26, name: '权杖5', nameEn: '5 of Wands', type: 'minor' },
  { id: 27, name: '权杖6', nameEn: '6 of Wands', type: 'minor' },
  { id: 28, name: '权杖7', nameEn: '7 of Wands', type: 'minor' },
  { id: 29, name: '权杖8', nameEn: '8 of Wands', type: 'minor' },
  { id: 30, name: '权杖9', nameEn: '9 of Wands', type: 'minor' },
  { id: 31, name: '权杖10', nameEn: '10 of Wands', type: 'minor' },
  { id: 32, name: '权杖侍从', nameEn: 'Page of Wands', type: 'minor' },
  { id: 33, name: '权杖骑士', nameEn: 'Knight of Wands', type: 'minor' },
  { id: 34, name: '权杖王后', nameEn: 'Queen of Wands', type: 'minor' },
  { id: 35, name: '权杖国王', nameEn: 'King of Wands', type: 'minor' },
  // 小阿尔卡那 - 圣杯 (14张)
  { id: 36, name: '圣杯ACE', nameEn: 'ACE of Cups', type: 'minor' },
  { id: 37, name: '圣杯2', nameEn: '2 of Cups', type: 'minor' },
  { id: 38, name: '圣杯3', nameEn: '3 of Cups', type: 'minor' },
  { id: 39, name: '圣杯4', nameEn: '4 of Cups', type: 'minor' },
  { id: 40, name: '圣杯5', nameEn: '5 of Cups', type: 'minor' },
  { id: 41, name: '圣杯6', nameEn: '6 of Cups', type: 'minor' },
  { id: 42, name: '圣杯7', nameEn: '7 of Cups', type: 'minor' },
  { id: 43, name: '圣杯8', nameEn: '8 of Cups', type: 'minor' },
  { id: 44, name: '圣杯9', nameEn: '9 of Cups', type: 'minor' },
  { id: 45, name: '圣杯10', nameEn: '10 of Cups', type: 'minor' },
  { id: 46, name: '圣杯侍从', nameEn: 'Page of Cups', type: 'minor' },
  { id: 47, name: '圣杯骑士', nameEn: 'Knight of Cups', type: 'minor' },
  { id: 48, name: '圣杯王后', nameEn: 'Queen of Cups', type: 'minor' },
  { id: 49, name: '圣杯国王', nameEn: 'King of Cups', type: 'minor' },
  // 小阿尔卡那 - 宝剑 (14张)
  { id: 50, name: '宝剑ACE', nameEn: 'ACE of Swords', type: 'minor' },
  { id: 51, name: '宝剑2', nameEn: '2 of Swords', type: 'minor' },
  { id: 52, name: '宝剑3', nameEn: '3 of Swords', type: 'minor' },
  { id: 53, name: '宝剑4', nameEn: '4 of Swords', type: 'minor' },
  { id: 54, name: '宝剑5', nameEn: '5 of Swords', type: 'minor' },
  { id: 55, name: '宝剑6', nameEn: '6 of Swords', type: 'minor' },
  { id: 56, name: '宝剑7', nameEn: '7 of Swords', type: 'minor' },
  { id: 57, name: '宝剑8', nameEn: '8 of Swords', type: 'minor' },
  { id: 58, name: '宝剑9', nameEn: '9 of Swords', type: 'minor' },
  { id: 59, name: '宝剑10', nameEn: '10 of Swords', type: 'minor' },
  { id: 60, name: '宝剑侍从', nameEn: 'Page of Swords', type: 'minor' },
  { id: 61, name: '宝剑骑士', nameEn: 'Knight of Swords', type: 'minor' },
  { id: 62, name: '宝剑王后', nameEn: 'Queen of Swords', type: 'minor' },
  { id: 63, name: '宝剑国王', nameEn: 'King of Swords', type: 'minor' },
  // 小阿尔卡那 - 星币 (14张)
  { id: 64, name: '星币ACE', nameEn: 'ACE of Pentacles', type: 'minor' },
  { id: 65, name: '星币2', nameEn: '2 of Pentacles', type: 'minor' },
  { id: 66, name: '星币3', nameEn: '3 of Pentacles', type: 'minor' },
  { id: 67, name: '星币4', nameEn: '4 of Pentacles', type: 'minor' },
  { id: 68, name: '星币5', nameEn: '5 of Pentacles', type: 'minor' },
  { id: 69, name: '星币6', nameEn: '6 of Pentacles', type: 'minor' },
  { id: 70, name: '星币7', nameEn: '7 of Pentacles', type: 'minor' },
  { id: 71, name: '星币8', nameEn: '8 of Pentacles', type: 'minor' },
  { id: 72, name: '星币9', nameEn: '9 of Pentacles', type: 'minor' },
  { id: 73, name: '星币10', nameEn: '10 of Pentacles', type: 'minor' },
  { id: 74, name: '星币侍从', nameEn: 'Page of Pentacles', type: 'minor' },
  { id: 75, name: '星币骑士', nameEn: 'Knight of Pentacles', type: 'minor' },
  { id: 76, name: '星币王后', nameEn: 'Queen of Pentacles', type: 'minor' },
  { id: 77, name: '星币国王', nameEn: 'King of Pentacles', type: 'minor' }
];

export default {
  name: 'App',
  setup() {
    const sessions = ref([]);
    const currentSessionId = ref(null);
    const inputValue = ref('');
    const isStreaming = ref(false);
    const messagesContainer = ref(null);
    const isSidebarOpen = ref(false);

    // 抽牌相关状态
    const showTarotDraw = ref(false);
    const tarotDrawConfig = ref(null);
    const selectedCards = ref([]);
    const shuffledCards = ref([]);

    let eventSource = null;

    // 当前会话
    const currentSession = computed(() => {
      return sessions.value.find(s => s.id === currentSessionId.value);
    });

    // 生成会话标题
    const generateSessionTitle = (firstMessage) => {
      if (!firstMessage) return '新对话';
      const preview = firstMessage.substring(0, 20);
      return preview.length < firstMessage.length ? preview + '...' : preview;
    };

    // 创建新会话
    const createNewSession = () => {
      const newSession = {
        id: Date.now(),
        memoryId: Math.floor(Math.random() * 1000000),
        title: '新对话',
        lastMessageTime: new Date().toLocaleTimeString('zh-CN', {
          hour: '2-digit',
          minute: '2-digit'
        }),
        messages: [{
          id: Date.now(),
          type: 'ai',
          content: '🔮 欢迎来到AI塔罗师!\n\n我可以帮你解读塔罗牌,探索你的问题。请告诉我你想了解什么,或者直接说"开始占卜"。',
          timestamp: new Date().toLocaleTimeString('zh-CN', {
            hour: '2-digit',
            minute: '2-digit'
          })
        }]
      };

      sessions.value.unshift(newSession);
      currentSessionId.value = newSession.id;
      isSidebarOpen.value = false;
      scrollToBottom();
    };

    // 切换会话
    const switchSession = (sessionId) => {
      if (isStreaming.value) {
        alert('请等待当前消息发送完成');
        return;
      }

      currentSessionId.value = sessionId;
      isSidebarOpen.value = false;
      scrollToBottom();
    };

    // 删除会话
    const deleteSession = (sessionId) => {
      if (sessions.value.length <= 1) {
        alert('至少保留一个会话');
        return;
      }

      const index = sessions.value.findIndex(s => s.id === sessionId);
      if (index === -1) return;

      sessions.value.splice(index, 1);

      // 如果删除的是当前会话,切换到第一个会话
      if (sessionId === currentSessionId.value) {
        currentSessionId.value = sessions.value[0].id;
      }
    };

    // 切换侧边栏
    const toggleSidebar = () => {
      isSidebarOpen.value = !isSidebarOpen.value;
    };

    // 滚动到底部
    const scrollToBottom = () => {
      nextTick(() => {
        if (messagesContainer.value) {
          messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight;
        }
      });
    };

    // 监听当前会话的消息变化
    watch(() => currentSession.value?.messages, () => {
      scrollToBottom();
    }, {deep: true});

    // 检测是否包含抽牌触发标记
    const detectTarotDrawTrigger = (content) => {
      // 1. 优先检测特殊标记
      const match = content.match(/\[TAROT_DRAW_START\](.*?)\[TAROT_DRAW_END\]/s);
      if (match) {
        try {
          const config = JSON.parse(match[1]);
          return config;
        } catch (e) {
          console.warn('抽牌配置解析失败', e);
        }
      }

      // 2. 兼容纯文字提示（包含选牌/抽牌/占卜等关键词就触发）
      const triggerKeywords = ['选牌', '抽牌', '占卜', '塔罗牌'];
      const hasKeyword = triggerKeywords.some(keyword => content.includes(keyword));
      if (hasKeyword) {
        return {
          message: content, // 使用AI的原始回复作为提示
          question: content,
          count: 3 // 默认抽3张牌
        };
      }

      return null;
    };

    // 洗牌
    const shuffleCards = (count) => {
      const shuffled = [...TAROT_CARDS].sort(() => Math.random() - 0.5);
      shuffledCards.value = shuffled.slice(0, Math.min(count * 10, 30)); // 显示更多牌供选择
    };

    // 判断卡片是否已选中
    const isCardSelected = (card) => {
      return selectedCards.value.find(c => c.id === card.id);
    };

    // 选择塔罗牌
    const handleCardSelect = (card) => {
      if (isCardSelected(card)) return;

      const newCard = {
        ...card,
        reversed: Math.random() > 0.5 // 随机正逆位
      };

      selectedCards.value.push(newCard);

      // 如果已选够牌,自动提交
      if (selectedCards.value.length >= (tarotDrawConfig.value?.count || 3)) {
        setTimeout(() => {
          confirmTarotDraw();
        }, 500);
      }
    };

    // 关闭抽牌界面
    const closeTarotDraw = () => {
      showTarotDraw.value = false;
      selectedCards.value = [];
      shuffledCards.value = [];
      tarotDrawConfig.value = null;
    };

    // 确认抽牌
    const confirmTarotDraw = () => {
      const cardsData = selectedCards.value.map((c, idx) => ({
        name: c.name,
        nameEn: c.nameEn,
        reversed: c.reversed,
        position: idx + 1
      }));

      // 构造发送给AI的消息
      const resultMessage = `用户抽取了以下塔罗牌:\n${cardsData.map(c =>
          `${c.position}. ${c.name}(${c.nameEn}) - ${c.reversed ? '逆位' : '正位'}`
      ).join('\n')}\n\n问题: ${tarotDrawConfig.value?.question || ''}`;

      // 关闭抽牌界面
      closeTarotDraw();

      // 发送结果给AI
      sendMessageToAI(resultMessage, true);
    };

    // 发送消息
    const handleSendMessage = () => {
      if (!inputValue.value.trim() || isStreaming.value || !currentSession.value) return;

      const messageToSend = inputValue.value;
      inputValue.value = '';
      sendMessageToAI(messageToSend, false);
    };

    // 发送消息到AI (统一处理)
    const sendMessageToAI = async (message, isSystemMessage = false) => {
      const session = currentSession.value;
      if (!session) return;


      // 如果是第一条用户消息,更新会话标题
      const isFirstUserMessage = session.messages.every(m => m.type === 'ai');
      if (isFirstUserMessage && !isSystemMessage) {
        session.title = generateSessionTitle(message);
      }

      // 添加用户消息 (系统消息不显示)
      if (!isSystemMessage) {
        const userMessage = {
          id: Date.now(),
          type: 'user',
          content: message,
          timestamp: new Date().toLocaleTimeString('zh-CN', {
            hour: '2-digit',
            minute: '2-digit'
          })
        };
        session.messages.push(userMessage);
        session.lastMessageTime = userMessage.timestamp;
      }

      scrollToBottom();

      // 创建AI消息占位符
      const aiMessage = {
        id: Date.now() + 1,
        type: 'ai',
        content: '',
        timestamp: new Date().toLocaleTimeString('zh-CN', {
          hour: '2-digit',
          minute: '2-digit'
        })
      };
      session.messages.push(aiMessage);

      isStreaming.value = true;

      try {
        // 使用 EventSource 进行 SSE 连接
        const url = `http://localhost:8081/api/ai/chat?memoryId=${session.memoryId}&message=${encodeURIComponent(message)}`;
        eventSource = new EventSource(url);

        eventSource.onmessage = (event) => {
          const chunk = event.data;
          const lastMessage = session.messages[session.messages.length - 1];
          if (lastMessage && lastMessage.type === 'ai') {
            lastMessage.content += chunk;

            // 检测是否触发抽牌
            const drawConfig = detectTarotDrawTrigger(lastMessage.content);
            if (drawConfig) {
              // 移除触发标记（如果有），保留提示消息
              lastMessage.content = lastMessage.content.replace(
                  /\[TAROT_DRAW_START\].*?\[TAROT_DRAW_END\]/s,
                  ''
              ).trim() || drawConfig.message;

              // 关键修复：使用nextTick确保状态更新触发视图渲染
              nextTick(() => {
                tarotDrawConfig.value = {
                  question: drawConfig.question || '',
                  message: drawConfig.message || '请选择塔罗牌',
                  count: drawConfig.count || 3
                };
                shuffleCards(tarotDrawConfig.value.count);
                showTarotDraw.value = true; // 强制显示抽牌界面
              });
            }
          }
        };

      } catch (error) {
        console.error('Error:', error);
        isStreaming.value = false;

        const lastMessage = session.messages[session.messages.length - 1];
        if (lastMessage && lastMessage.type === 'ai') {
          lastMessage.content = '发送消息失败,请检查网络连接和后端服务状态。';
        }
      }
    };

    onMounted(() => {
      // 创建初始会话
      createNewSession();
    });

    return {
      // 响应式状态
      sessions,
      currentSessionId,
      currentSession,
      inputValue,
      isStreaming,
      messagesContainer,
      isSidebarOpen,
      showTarotDraw,
      tarotDrawConfig,
      selectedCards,
      shuffledCards,
      // 方法 (关键修复：添加模板中调用的所有方法)
      createNewSession,
      switchSession,
      deleteSession,
      toggleSidebar,
      handleSendMessage,
      isCardSelected,
      handleCardSelect,
      closeTarotDraw,
      confirmTarotDraw
    };
  } // 修复：拆分闭合花括号，不再连写
}; // 修复：正确闭合export default
</script>
<style scoped>
/* 全局容器样式 */
.app-container {
  display: flex;
  height: 100vh;
  width: 100vw;
  overflow: hidden;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
}

/* 侧边栏样式 */
.sidebar {
  width: 280px;
  background-color: #f8fafc;
  border-right: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
  transition: transform 0.3s ease;
  z-index: 10;
}

.sidebar-open {
  transform: translateX(0);
}

/* 移动端侧边栏隐藏/显示 */
@media (max-width: 768px) {
  .sidebar {
    position: fixed;
    height: 100vh;
    transform: translateX(-100%);
  }

  .mobile-menu-btn {
    display: block !important;
  }
}

/* 侧边栏头部 */
.sidebar-header {
  padding: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #e2e8f0;
}

.logo-section {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: #1e293b;
  font-size: 16px;
}

.sparkles-icon {
  color: #8b5cf6;
}

.new-chat-btn {
  background: transparent;
  border: none;
  cursor: pointer;
  color: #64748b;
  padding: 8px;
  border-radius: 6px;
  transition: background-color 0.2s;
}

.new-chat-btn:hover {
  background-color: #e2e8f0;
  color: #1e293b;
}

/* 会话列表 */
.sessions-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.session-item {
  padding: 12px;
  border-radius: 6px;
  margin-bottom: 4px;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: center;
  transition: background-color 0.2s;
}

.session-item:hover {
  background-color: #e2e8f0;
}

.session-item.active {
  background-color: #ede9fe;
  color: #8b5cf6;
}

.session-info {
  flex: 1;
  overflow: hidden;
}

.session-title {
  font-size: 14px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.session-time {
  font-size: 12px;
  color: #64748b;
  margin-top: 2px;
}

.delete-btn {
  background: transparent;
  border: none;
  cursor: pointer;
  color: #94a3b8;
  padding: 4px;
  border-radius: 4px;
  opacity: 0;
  transition: all 0.2s;
}

.session-item:hover .delete-btn {
  opacity: 1;
}

.delete-btn:hover {
  color: #ef4444;
  background-color: #fef2f2;
}

/* 主内容区 */
.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
}

.mobile-menu-btn {
  display: none;
  position: absolute;
  top: 16px;
  left: 16px;
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 8px;
  cursor: pointer;
  z-index: 5;
}

/* 头部 */
.header {
  padding: 16px 24px;
  border-bottom: 1px solid #e2e8f0;
  background-color: white;
}

.header-content h1 {
  font-size: 20px;
  font-weight: 600;
  color: #1e293b;
  margin: 0;
}

.session-id {
  font-size: 12px;
  color: #64748b;
  margin: 4px 0 0 0;
}

/* 消息区域 */
.messages-area {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
  background-color: white;
}

.messages-wrapper {
  max-width: 800px;
  margin: 0 auto;
}

.message-row {
  display: flex;
  margin-bottom: 16px;
}

.message-left {
  justify-content: flex-start;
}

.message-right {
  justify-content: flex-end;
}

.message-bubble {
  max-width: 70%;
  padding: 12px 16px;
  border-radius: 16px;
  position: relative;
}

.message-bubble.user {
  background-color: #8b5cf6;
  color: white;
  border-bottom-right-radius: 4px;
}

.message-bubble.ai {
  background-color: #f1f5f9;
  color: #1e293b;
  border-bottom-left-radius: 4px;
}

.message-content {
  font-size: 14px;
  line-height: 1.5;
}

.message-time {
  font-size: 10px;
  color: #94a3b8;
  margin-top: 4px;
  opacity: 0.8;
}

.message-bubble.user .message-time {
  color: #e0d5ff;
  text-align: right;
}

/* 加载动画 */
.loading-dots {
  display: flex;
  gap: 4px;
  padding: 8px 0;
}

.loading-dots span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #64748b;
  animation: loading 1.4s infinite ease-in-out both;
}

.loading-dots span:nth-child(1) {
  animation-delay: -0.32s;
}

.loading-dots span:nth-child(2) {
  animation-delay: -0.16s;
}

@keyframes loading {
  0%, 80%, 100% {
    transform: scale(0);
  }
  40% {
    transform: scale(1);
  }
}

/* 输入区域 */
.input-area {
  padding: 16px 24px;
  border-top: 1px solid #e2e8f0;
  background-color: white;
}

.input-wrapper {
  max-width: 800px;
  margin: 0 auto;
  display: flex;
  gap: 8px;
}

.message-input {
  flex: 1;
  padding: 12px 16px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s;
}

.message-input:focus {
  border-color: #8b5cf6;
}

.message-input:disabled {
  background-color: #f8fafc;
  cursor: not-allowed;
}

.send-button {
  background-color: #8b5cf6;
  color: white;
  border: none;
  border-radius: 12px;
  padding: 12px 20px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
  transition: background-color 0.2s;
}

.send-button:disabled {
  background-color: #c4b5fd;
  cursor: not-allowed;
}

.send-button:hover:not(:disabled) {
  background-color: #7c3aed;
}

/* 抽牌弹窗遮罩 */
.tarot-draw-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
  padding: 16px;
}

.tarot-draw-modal {
  background-color: white;
  border-radius: 12px;
  width: 100%;
  max-width: 800px;
  max-height: 90vh;
  overflow-y: auto;
  padding: 24px;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1);
}

.tarot-header {
  margin-bottom: 24px;
  text-align: center;
}

.tarot-header h2 {
  color: #1e293b;
  margin: 0 0 8px 0;
}

.tarot-message {
  color: #64748b;
  margin: 0 0 4px 0;
}

.tarot-progress {
  font-size: 14px;
  color: #8b5cf6;
  font-weight: 500;
  margin: 8px 0 0 0;
}

/* 塔罗牌网格 */
.tarot-cards-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(80px, 1fr));
  gap: 12px;
  margin-bottom: 24px;
}

.tarot-card {
  aspect-ratio: 2/3;
  background-color: #f8fafc;
  border: 2px solid #e2e8f0;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: bold;
  color: #64748b;
  transition: all 0.2s;
}

.tarot-card:hover:not(:disabled) {
  border-color: #8b5cf6;
  background-color: #f5f3ff;
}

.tarot-card.selected {
  background-color: #8b5cf6;
  border-color: #7c3aed;
  color: white;
}

.tarot-card:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 已选卡牌信息 */
.selected-cards-info {
  margin-bottom: 24px;
  padding: 16px;
  background-color: #f8fafc;
  border-radius: 8px;
}

.selected-cards-info h3 {
  color: #1e293b;
  margin: 0 0 12px 0;
  font-size: 16px;
}

.selected-cards-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 8px;
}

.selected-card-item {
  padding: 8px 12px;
  background-color: white;
  border-radius: 6px;
  border-left: 3px solid #8b5cf6;
  font-size: 14px;
}

/* 抽牌操作按钮 */
.tarot-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-top: 16px;
}

.tarot-btn {
  padding: 12px 24px;
  border-radius: 8px;
  font-weight: 500;
  cursor: pointer;
  border: none;
  font-size: 14px;
  transition: background-color 0.2s;
}

.tarot-btn-cancel {
  background-color: #f8fafc;
  color: #64748b;
}

.tarot-btn-cancel:hover {
  background-color: #e2e8f0;
}

.tarot-btn-confirm {
  background-color: #8b5cf6;
  color: white;
}

.tarot-btn-confirm:hover {
  background-color: #7c3aed;
}

/* 移动端遮罩层 */
.overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  z-index: 5;
  display: none;
}

@media (max-width: 768px) {
  .overlay {
    display: block;
  }
}
</style>