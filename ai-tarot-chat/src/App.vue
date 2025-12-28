<template>
  <div class="chat-container">
    <!-- Header -->
    <div class="header">
      <div class="header-content">
        <svg class="sparkles-icon" xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M9.937 15.5A2 2 0 0 0 8.5 14.063l-6.135-1.582a.5.5 0 0 1 0-.962L8.5 9.936A2 2 0 0 0 9.937 8.5l1.582-6.135a.5.5 0 0 1 .963 0L14.063 8.5A2 2 0 0 0 15.5 9.937l6.135 1.581a.5.5 0 0 1 0 .964L15.5 14.063a2 2 0 0 0-1.437 1.437l-1.582 6.135a.5.5 0 0 1-.963 0z"/>
          <path d="M20 3v4"/>
          <path d="M22 5h-4"/>
          <path d="M4 17v2"/>
          <path d="M5 18H3"/>
        </svg>
        <div>
          <h1>AI塔罗师</h1>
          <p class="session-id">会话ID: {{ memoryId }}</p>
        </div>
      </div>
    </div>

    <!-- Messages Area -->
    <div class="messages-area" ref="messagesContainer">
      <div class="messages-wrapper">
        <div
            v-for="msg in messages"
            :key="msg.id"
            :class="['message-row', msg.type === 'user' ? 'message-right' : 'message-left']"
        >
          <div :class="['message-bubble', msg.type]">
            <div class="message-content">{{ msg.content }}</div>
            <div class="message-time">{{ msg.timestamp }}</div>
          </div>
        </div>

        <!-- Loading indicator -->
        <div v-if="isStreaming && messages.length > 0 && messages[messages.length - 1].type === 'ai'" class="message-row message-left">
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
</template>

<script>
import { ref, onMounted, nextTick, onUnmounted, watch } from 'vue';

export default {
  name: 'App',
  setup() {
    const messages = ref([]);
    const inputValue = ref('');
    const isStreaming = ref(false);
    const memoryId = ref(Math.floor(Math.random() * 1000000));
    const messagesContainer = ref(null);
    let eventSource = null;

    const scrollToBottom = () => {
      nextTick(() => {
        if (messagesContainer.value) {
          messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight;
        }
      });
    };

    // 监听消息变化,自动滚动
    watch(messages, () => {
      scrollToBottom();
    }, { deep: true });

    const handleSendMessage = async () => {
      if (!inputValue.value.trim() || isStreaming.value) return;

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
      messages.value.push(userMessage);

      const messageToSend = inputValue.value;
      inputValue.value = '';
      scrollToBottom();

      // 创建AI消息占位符
      const aiMessageId = Date.now() + 1;
      const aiMessage = {
        id: aiMessageId,
        type: 'ai',
        content: '',
        timestamp: new Date().toLocaleTimeString('zh-CN', {
          hour: '2-digit',
          minute: '2-digit'
        })
      };
      messages.value.push(aiMessage);

      isStreaming.value = true;

      try {
        // 使用 EventSource 进行 SSE 连接
        const url = `http://localhost:8081/api/ai/chat?memoryId=${memoryId.value}&message=${encodeURIComponent(messageToSend)}`;
        eventSource = new EventSource(url);

        eventSource.onmessage = (event) => {
          const chunk = event.data;

          // 更新最后一条AI消息
          const lastMessage = messages.value[messages.value.length - 1];
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

          const lastMessage = messages.value[messages.value.length - 1];
          if (lastMessage && lastMessage.type === 'ai' && !lastMessage.content) {
            lastMessage.content = '抱歉,连接出现问题。请确保后端服务正在运行(http://localhost:8081)';
          }
        };

        // 监听结束事件 (如果后端发送)
        eventSource.addEventListener('end', () => {
          if (eventSource) {
            eventSource.close();
          }
          isStreaming.value = false;
        });

      } catch (error) {
        console.error('Error:', error);
        isStreaming.value = false;

        const lastMessage = messages.value[messages.value.length - 1];
        if (lastMessage && lastMessage.type === 'ai') {
          lastMessage.content = '发送消息失败,请检查网络连接和后端服务状态。';
        }
      }
    };

    onMounted(() => {
      // 欢迎消息
      messages.value.push({
        id: Date.now(),
        type: 'ai',
        content: '🔮 欢迎来到AI塔罗师!\n\n我可以帮你解读塔罗牌,探索你的问题。请告诉我你想了解什么,或者直接说"开始占卜"。',
        timestamp: new Date().toLocaleTimeString('zh-CN', {
          hour: '2-digit',
          minute: '2-digit'
        })
      });
      scrollToBottom();
    });

    onUnmounted(() => {
      if (eventSource) {
        eventSource.close();
      }
    });

    return {
      messages,
      inputValue,
      isStreaming,
      memoryId,
      messagesContainer,
      handleSendMessage
    };
  }
};
</script>

<style scoped>
* {
  box-sizing: border-box;
}

.chat-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: linear-gradient(to bottom right, #581c87, #3730a3, #1e3a8a);
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

/* Header */
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
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.sparkles-icon {
  width: 2rem;
  height: 2rem;
  color: #d8b4fe;
  flex-shrink: 0;
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
  margin: 0;
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

/* Loading dots */
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
.messages-area::-webkit-scrollbar {
  width: 8px;
}

.messages-area::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.1);
}

.messages-area::-webkit-scrollbar-thumb {
  background: rgba(124, 58, 237, 0.5);
  border-radius: 4px;
}

.messages-area::-webkit-scrollbar-thumb:hover {
  background: rgba(124, 58, 237, 0.7);
}

/* 响应式设计 */
@media (max-width: 640px) {
  .header-content,
  .messages-wrapper,
  .input-wrapper {
    padding-left: 0.5rem;
    padding-right: 0.5rem;
  }

  .message-bubble {
    max-width: 85%;
  }

  .send-button {
    padding: 0.75rem 1rem;
  }
}
</style>