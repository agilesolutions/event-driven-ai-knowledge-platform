package com.agilesolutions.intelligence.init;

import com.agilesolutions.intelligence.document.NotebookIntelligenceDocument;
import com.agilesolutions.intelligence.service.NotebookVectorIndexService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class QdrantDataLoader implements ApplicationRunner {

    private final NotebookVectorIndexService vectorIndexService;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;


    @Override
    public void run(ApplicationArguments args) throws Exception {

        log.info("Initializing Qdrant vector initialization process...");

        if (args.containsOption("skip-vector-seed")) {
            log.info("Vector seeding bypassed via runtime argument command-line flags.");
            return;
        }

        try {
            // Point directly to resource path mapping the legacy layout structures
            Resource resource = resourceLoader.getResource("classpath:data/notebook-seed.json");

            if (!resource.exists()) {
                log.warn("Initialization aborted: 'classpath:data/notebook-seed.json' does not exist.");
                return;
            }

            try (InputStream inputStream = resource.getInputStream()) {
                // Deserialize cleanly using configured Object Mapper instances
                List<NotebookIntelligenceDocument> documents = objectMapper.readValue(
                        inputStream,
                        new TypeReference<List<NotebookIntelligenceDocument>>() {}
                );

                log.info("Extracted {} records from JSON payload. Feeding data to vector pipeline...", documents.size());

                // Execute target loop directly using the native index signature block
                for (NotebookIntelligenceDocument notebookDoc : documents) {
                    try {
                        vectorIndexService.index(notebookDoc);
                        log.info("Successfully vectorized document ID: {}", notebookDoc.getNotebookId());
                    } catch (IllegalArgumentException e) {
                        log.error("Skipping document initialization anomaly for ID {}: {}",
                                notebookDoc.getNotebookId(), e.getMessage());
                    }
                }

                log.info("Qdrant semantic engine data initialization cycle finished successfully.");
            }
        } catch (Exception e) {
            log.error("Critical runtime vector database ingestion sequence aborted", e);
        }
    };

}
