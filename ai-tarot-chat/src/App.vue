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

export default {
  name: 'App',
  setup() {
    const sessions = ref([]);
    const currentSessionId = ref(null);
    const inputValue = ref('');
    const isStreaming = ref(false);
    const messagesContainer = ref(null);
    const isSidebarOpen = ref(false);
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
    }, { deep: true });

    // 发送消息
    const handleSendMessage = async () => {
      if (!inputValue.value.trim() || isStreaming.value || !currentSession.value) return;

      const session = currentSession.value;

      // 如果是第一条用户消息,更新会话标题
      const isFirstUserMessage = session.messages.every(m => m.type === 'ai');
      if (isFirstUserMessage) {
        session.title = generateSessionTitle(inputValue.value);
      }

      // 添加用户消息
      const userMessage = {
        id: Date.now(),
        type: 'user',
        content: inputValue.value,
        timestamp: new Date().toLocaleTimeString('zh-CN', {
          hour: '2-digit',
          minute: '2-digit'
        })
      };
      session.messages.push(userMessage);

      // 更新会话时间
      session.lastMessageTime = userMessage.timestamp;

      const messageToSend = inputValue.value;
      inputValue.value = '';
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
        const url = `http://localhost:8081/api/ai/chat?memoryId=${session.memoryId}&message=${encodeURIComponent(messageToSend)}`;
        eventSource = new EventSource(url);

        eventSource.onmessage = (event) => {
          const chunk = event.data;
          const lastMessage = session.messages[session.messages.length - 1];
          if (lastMessage && lastMessage.type === 'ai') {
            lastMessage.content += chunk;
          }
        };

        eventSource.onerror = (error) => {
          console.error('SSE Error:', error);
          if (eventSource) {
            eventSource.close();
          }
          isStreaming.value = false;

          const lastMessage = session.messages[session.messages.length - 1];
          if (lastMessage && lastMessage.type === 'ai' && !lastMessage.content) {
            lastMessage.content = '抱歉,连接出现问题。请确保后端服务正在运行(http://localhost:8081)';
          }
        };

        eventSource.addEventListener('end', () => {
          if (eventSource) {
            eventSource.close();
          }
          isStreaming.value = false;
        });

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
      sessions,
      currentSessionId,
      currentSession,
      inputValue,
      isStreaming,
      messagesContainer,
      isSidebarOpen,
      createNewSession,
      switchSession,
      deleteSession,
      toggleSidebar,
      handleSendMessage
    };
  }
};
</script>

<style scoped>
* {
  box-sizing: border-box;
}

.app-container {
  display: flex;
  height: 100vh;
  background: linear-gradient(to bottom right, #581c87, #3730a3, #1e3a8a);
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

/* ========== 左侧边栏 ========== */
.sidebar {
  width: 280px;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-right: 1px solid rgba(167, 139, 250, 0.2);
  display: flex;
  flex-direction: column;
  transition: transform 0.3s ease;
}

.sidebar-header {
  padding: 1rem;
  border-bottom: 1px solid rgba(167, 139, 250, 0.2);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
}

.logo-section {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: white;
  font-weight: 600;
  font-size: 1rem;
}

.logo-section .sparkles-icon {
  width: 1.5rem;
  height: 1.5rem;
  color: #d8b4fe;
}

.new-chat-btn {
  background: rgba(124, 58, 237, 0.8);
  color: white;
  border: none;
  border-radius: 0.5rem;
  padding: 0.5rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  flex-shrink: 0;
}

.new-chat-btn:hover {
  background: rgba(124, 58, 237, 1);
  transform: scale(1.05);
}

.sessions-list {
  flex: 1;
  overflow-y: auto;
  padding: 0.5rem;
}

.session-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.75rem;
  margin-bottom: 0.5rem;
  border-radius: 0.5rem;
  cursor: pointer;
  transition: all 0.2s;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid transparent;
}

.session-item:hover {
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(167, 139, 250, 0.3);
}

.session-item.active {
  background: rgba(124, 58, 237, 0.3);
  border-color: rgba(124, 58, 237, 0.5);
}

.session-info {
  flex: 1;
  min-width: 0;
}

.session-title {
  color: white;
  font-size: 0.875rem;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 0.25rem;
}

.session-time {
  color: rgba(216, 180, 254, 0.7);
  font-size: 0.75rem;
}

.delete-btn {
  background: transparent;
  border: none;
  color: rgba(239, 68, 68, 0.7);
  cursor: pointer;
  padding: 0.25rem;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 0.25rem;
  opacity: 0;
  transition: all 0.2s;
  flex-shrink: 0;
}

.session-item:hover .delete-btn {
  opacity: 1;
}

.delete-btn:hover {
  background: rgba(239, 68, 68, 0.2);
  color: rgba(239, 68, 68, 1);
}

/* ========== 主内容区 ========== */
.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  position: relative;
}

.mobile-menu-btn {
  display: none;
  position: absolute;
  top: 1rem;
  left: 1rem;
  z-index: 100;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(167, 139, 250, 0.3);
  color: white;
  border-radius: 0.5rem;
  padding: 0.5rem;
  cursor: pointer;
}

.header {
  background: rgba(0, 0, 0, 0.3);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-bottom: 1px solid rgba(167, 139, 250, 0.3);
  padding: 1rem;
}

.header-content {
  max-width: 56rem;
  margin: 0 auto;
}

.header h1 {
  color: white;
  font-size: 1.5rem;
  line-height: 2rem;
  margin: 0;
  font-weight: 700;
}

.session-id {
  color: #d8b4fe;
  font-size: 0.875rem;
  line-height: 1.25rem;
  margin: 0.25rem 0 0;
}

/* Messages Area */
.messages-area {
  flex: 1;
  overflow-y: auto;
  padding: 1rem;
}

.messages-wrapper {
  max-width: 56rem;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.message-row {
  display: flex;
  width: 100%;
}

.message-left {
  justify-content: flex-start;
}

.message-right {
  justify-content: flex-end;
}

.message-bubble {
  max-width: 70%;
  padding: 1rem;
  border-radius: 1rem;
  word-wrap: break-word;
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1);
}

.message-bubble.user {
  background: #7c3aed;
  color: white;
}

.message-bubble.ai {
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  color: white;
  border: 1px solid rgba(167, 139, 250, 0.3);
}

.message-content {
  white-space: pre-wrap;
  line-height: 1.5;
  word-break: break-word;
}

.message-time {
  font-size: 0.75rem;
  line-height: 1rem;
  margin-top: 0.5rem;
  opacity: 0.7;
}

.message-bubble.loading {
  padding: 1rem 1.5rem;
}

.loading-dots {
  display: flex;
  gap: 0.25rem;
  align-items: center;
}

.loading-dots span {
  width: 0.5rem;
  height: 0.5rem;
  background: #a78bfa;
  border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out both;
}

.loading-dots span:nth-child(1) {
  animation-delay: -0.32s;
}

.loading-dots span:nth-child(2) {
  animation-delay: -0.16s;
}

@keyframes bounce {
  0%, 80%, 100% {
    transform: scale(0);
    opacity: 0.5;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

/* Input Area */
.input-area {
  background: rgba(0, 0, 0, 0.3);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-top: 1px solid rgba(167, 139, 250, 0.3);
  padding: 1rem;
}

.input-wrapper {
  max-width: 56rem;
  margin: 0 auto;
  display: flex;
  gap: 0.75rem;
}

.message-input {
  flex: 1;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(167, 139, 250, 0.3);
  border-radius: 0.75rem;
  padding: 0.75rem 1rem;
  color: white;
  font-size: 1rem;
  line-height: 1.5rem;
  outline: none;
  transition: all 0.2s;
}

.message-input::placeholder {
  color: #d8b4fe;
  opacity: 0.6;
}

.message-input:focus {
  border-color: #7c3aed;
  box-shadow: 0 0 0 3px rgba(124, 58, 237, 0.1);
}

.message-input:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.send-button {
  background: #7c3aed;
  color: white;
  border: none;
  border-radius: 0.75rem;
  padding: 0.75rem 1.5rem;
  font-size: 1rem;
  line-height: 1.5rem;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  transition: all 0.2s;
  white-space: nowrap;
}

.send-button:hover:not(:disabled) {
  background: #6d28d9;
}

.send-button:disabled {
  background: #6b7280;
  cursor: not-allowed;
  opacity: 0.6;
}

.send-button svg {
  flex-shrink: 0;
}

/* Scrollbar */
.sessions-list::-webkit-scrollbar,
.messages-area::-webkit-scrollbar {
  width: 6px;
}

.sessions-list::-webkit-scrollbar-track,
.messages-area::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.1);
}

.sessions-list::-webkit-scrollbar-thumb,
.messages-area::-webkit-scrollbar-thumb {
  background: rgba(124, 58, 237, 0.5);
  border-radius: 3px;
}

.sessions-list::-webkit-scrollbar-thumb:hover,
.messages-area::-webkit-scrollbar-thumb:hover {
  background: rgba(124, 58, 237, 0.7);
}

/* 遮罩层 */
.overlay {
  display: none;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .sidebar {
    position: fixed;
    left: 0;
    top: 0;
    height: 100vh;
    z-index: 1000;
    transform: translateX(-100%);
  }

  .sidebar.sidebar-open {
    transform: translateX(0);
  }

  .overlay {
    display: block;
    position: fixed;
    inset: 0;
    background: rgba(0, 0, 0, 0.5);
    z-index: 999;
  }

  .mobile-menu-btn {
    display: flex;
  }

  .header {
    padding-left: 4rem;
  }

  .message-bubble {
    max-width: 85%;
  }

  .send-button {
    padding: 0.75rem 1rem;
  }
}
</style>