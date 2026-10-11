
package com.agilesolutions.intelligence.processor;

import com.agilesolutions.intelligence.document.NotebookIntelligenceDocument;
import com.agilesolutions.intelligence.domain.NotebookEnrichmentResult;
import com.agilesolutions.intelligence.repository.NotebookIntelligenceRepository;
import com.agilesolutions.intelligence.service.NotebookAiEnrichmentService;
import com.agilesolutions.intelligence.service.NotebookVectorIndexService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.UUID;

/**
 * Processes notebook creation events into enriched intelligence documents.
 *
 * MongoDB is the authoritative document store; Qdrant provides semantic
 * retrieval over the enriched content.
 */
@Service
@RequiredArgsConstructor
public class NotebookIntelligenceProcessor {

    private final NotebookAiEnrichmentService aiEnrichmentService;
    private final NotebookIntelligenceRepository repository;
    private final NotebookVectorIndexService vectorIndexService;

    /**
     * Processes a notebook creation event.
     *
     * @param notebookId stable ID from the source Notebook service
     * @param title notebook title from the event
     * @param description notebook description from the event
     */
    public void process(
            String notebookId,
            String title,
            String description) {

        validateNotebook(notebookId, title);

        // 1. Enrich notebook content using Spring AI.
        NotebookEnrichmentResult enrichment =
                aiEnrichmentService.enrich(title, description);

        // 2. Build the MongoDB document.
        Instant now = Instant.now();

        NotebookIntelligenceDocument document =
                repository.findById(notebookId)
                        .orElseGet(NotebookIntelligenceDocument::new);

        boolean isNew = document.getCreatedAt() == null;

        document.setNotebookId(notebookId);
        document.setTitle(title.trim());
        document.setDescription(description);
        document.setSummary(enrichment.summary());
        document.setTopics(enrichment.topics());
        document.setKeywords(enrichment.keywords());
        document.setEntities(enrichment.entities().stream()
                .map(entity -> {
                    var extracted =
                            new NotebookIntelligenceDocument.ExtractedEntity();
                    extracted.setName(entity.name());
                    extracted.setType(entity.type());
                    return extracted;
                })
                .toList());

        document.setStatus("COMPLETED");
        document.setUpdatedAt(now);

        if (isNew) {
            document.setCreatedAt(now);
        }

        document.setEnrichedAt(now);

        // 3. Persist the canonical document first.
        repository.save(document);

        // 4. Index the persisted content in Qdrant.
        // If this fails, the MongoDB document remains available.
        vectorIndexService.index(document);
    }

    private void validateNotebook(String notebookId, String title) {
        if (!StringUtils.hasText(notebookId)) {
            throw new IllegalArgumentException(
                    "Notebook ID must not be blank");
        }

        if (!StringUtils.hasText(title)) {
            throw new IllegalArgumentException(
                    "Notebook title must not be blank");
        }

        // Validate UUID format if the source API guarantees UUID identifiers.
        try {
            UUID.fromString(notebookId);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Notebook ID must be a valid UUID: " + notebookId,
                    exception);
        }
    }

    /**
     * Retries vector indexing without repeating AI enrichment.
     *
     * @param notebookId ID of the persisted intelligence document
     */
    public void retryVectorIndexing(String notebookId) {
        if (!StringUtils.hasText(notebookId)) {
            throw new IllegalArgumentException(
                    "Notebook ID must not be blank");
        }

        NotebookIntelligenceDocument document = repository.findById(notebookId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Notebook intelligence document not found: " + notebookId));

        vectorIndexService.index(document);
    }
}