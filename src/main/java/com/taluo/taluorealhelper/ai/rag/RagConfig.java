package com.taluo.taluorealhelper.ai.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.FileSystem;
import java.util.List;

@Configuration
public class RagConfig {

    @Resource
    private EmbeddingModel embeddingModel;

    @Resource
    private EmbeddingStore<TextSegment> embeddingStore;

    @Bean
    public ContentRetriever contentRetriever() {
        //------RAG------
        //1. 加载文档
        List<Document> documents = FileSystemDocumentLoader.loadDocuments("src/main/resources/docs");
        //2.文档切割 约束1000个字符，重复200个
        DocumentByParagraphSplitter splitter =
                new DocumentByParagraphSplitter(1000, 200);
        //3.自定义文件加载器，把文档转化成向量并保存到数据向量库
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(splitter)
                // 增强文档质量，为切割过后的文档添加文档名
                .textSegmentTransformer(textSegment ->
                        TextSegment.from(
                                textSegment.metadata().getString("file_name") + "\n" + textSegment.text()
                                , textSegment.metadata()))
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();
        //4.加载文档
        ingestor.ingest(documents);
        //5. 自定义内容加载器
        EmbeddingStoreContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(5)//最多5条
                .minScore(0.75)//过滤分数小于0.75的结果
                .build();
        return contentRetriever;
    }
}
