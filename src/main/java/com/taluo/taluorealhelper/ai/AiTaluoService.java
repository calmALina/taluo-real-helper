package com.taluo.taluorealhelper.ai;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import reactor.core.publisher.Flux;

import java.util.List;

public interface AiTaluoService {


    @SystemMessage(fromResource = "system-prompt.txt")
    String chat(String message);

    @SystemMessage(fromResource = "system-prompt.txt")
    Report chatReport(String message);

    @SystemMessage(fromResource = "system-prompt.txt")
    record Report(String name, List<String> suggestionsList){};

    @SystemMessage(fromResource = "system-prompt.txt")
    Flux<String> chatStream(@MemoryId int memoryId, @UserMessage String message);
}
