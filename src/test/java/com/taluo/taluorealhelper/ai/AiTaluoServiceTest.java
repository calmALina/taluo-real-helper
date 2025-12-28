package com.taluo.taluorealhelper.ai;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AiTaluoServiceTest {

    @Resource
    private AiTaluoService aiTaluoService;

    @Test
    void chat(){
        //第一次对话
        String chat = aiTaluoService.chat("你好，我是mimi");
        System.out.println(chat);
        //第二次对话
        chat=aiTaluoService.chat("我刚刚说我是谁？");
        System.out.println(chat);
    }

    @Test
    void chatReport(){
        AiTaluoService.Report report = aiTaluoService.chatReport("你好，我是mimi,请帮我算术塔罗");
        System.out.println(report);
    }

    @Test
    void chatRag(){
        String chat = aiTaluoService.chat("你好，我是mimi,我怎么做一个塔罗师？");
        System.out.println(chat);
    }

    @Test
    void chatTool(){
        String chat = aiTaluoService.chat("今天有没有什么推荐算法？");
        System.out.println(chat);
    }

    @Test
    void chatMCP(){
        String chat = aiTaluoService.chat("帮我查一下2009年11月3日是星期几");
        System.out.println(chat);
    }
    @Test
    void chatGuardrail(){
        String chat = aiTaluoService.chat("帮我算命");
        System.out.println(chat);
    }
}