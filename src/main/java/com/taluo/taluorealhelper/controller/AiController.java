package com.taluo.taluorealhelper.controller;

import com.taluo.taluorealhelper.ai.AiTaluoService;
import dev.langchain4j.service.spring.AiService;
import jakarta.annotation.Resource;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("ai")
public class AiController {

    @Resource
    private AiTaluoService aiTaluoService;

    @GetMapping("/Chat")
    public Flux<ServerSentEvent<String>> chat(int memoryId,String message) {
        return aiTaluoService.chatStream(memoryId, message)
                .map(chunk -> ServerSentEvent.<String>builder()
                        .data(chunk)
                        .build());
    }
}
