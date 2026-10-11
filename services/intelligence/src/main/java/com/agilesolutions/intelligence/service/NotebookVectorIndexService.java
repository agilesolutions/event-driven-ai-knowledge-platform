
package com.agilesolutions.intelligence.service;

import com.agilesolutions.intelligence.document.NotebookIntelligenceDocument;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Maintains the Qdrant vector representation of MongoDB notebook
 * intelligence documents.
 *
 * MongoDB remains the authoritative document store. Qdrant stores
 * searchable text, embeddings and metadata used to locate MongoDB records.
 */
@Service
public class NotebookVectorIndexService {

    private static final int MAX_CONTENT_LENGTH = 20_000;

    private final VectorStore vectorStore;

    public NotebookVectorIndexService(VectorStore vectorStore) {
        this.vectorStore = Objects.requireNonNull(
                vectorStore, "vectorStore must not be null");
    }

    /**
     * Creates or refreshes the searchable representation of a notebook.
     *
     * The document ID is derived from the stable notebook ID, so repeated
     * indexing does not intentionally create a new logical document.
     *
     * @param notebook the persisted MongoDB intelligence document
     */
    public void index(NotebookIntelligenceDocument notebook) {
        Objects.requireNonNull(notebook, "notebook must not be null");

        String notebookId = requireText(
                notebook.getNotebookId(), "notebookId");

        String content = buildContent(notebook);

        if (!StringUtils.hasText(content)) {
            throw new IllegalArgumentException(
                    "Notebook has no searchable content: " + notebookId);
        }

        Map<String, Object> metadata = buildMetadata(notebook, notebookId);

        Document vectorDocument = new Document(
                notebookId,
                content,
                metadata
        );

        /*
         * VectorStore.add() generates embeddings using the configured
         * EmbeddingModel and writes the document to the vector store.
         *
         * Verify replacement/upsert semantics for the Spring AI version
         * and Qdrant integration in use. If add() does not replace an
         * existing point with the same ID, delete by ID before adding.
         */
        vectorStore.add(List.of(vectorDocument));
    }

    /**
     * Removes a notebook from the vector index.
     *
     * Call this when a notebook is deleted or is no longer eligible
     * for semantic search.
     */
    public void delete(String notebookId) {
        String id = requireText(notebookId, "notebookId");
        vectorStore.delete(List.of(id));
    }

    /**
     * Finds semantically similar notebooks.
     *
     * Returned documents contain indexed content and metadata. Load the
     * complete, authoritative documents from MongoDB using the notebookId
     * metadata rather than returning the vector document as the API DTO.
     *
     * @param query natural-language search text
     * @param topK maximum number of matches
     * @return matching vector documents, ordered by similarity
     */
    public List<Document> search(String query, int topK) {
        String searchText = requireText(query, "query");

        if (topK < 1 || topK > 100) {
            throw new IllegalArgumentException(
                    "topK must be between 1 and 100");
        }

        return vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(searchText)
                        .topK(topK)
                        .build()
        );
    }

    /**
     * Builds the text to embed. Combining the title, summary and extracted
     * topics gives semantic retrieval more context than the title alone.
     */
    private String buildContent(NotebookIntelligenceDocument notebook) {
        List<String> sections = new ArrayList<>();

        addSection(sections, "Title", notebook.getTitle());
        addSection(sections, "Description", notebook.getDescription());
        addSection(sections, "Summary", notebook.getSummary());
        addSection(sections, "Topics", join(notebook.getTopics()));
        addSection(sections, "Keywords", join(notebook.getKeywords()));

        String content = String.join("\n", sections).trim();

        if (content.length() > MAX_CONTENT_LENGTH) {
            content = content.substring(0, MAX_CONTENT_LENGTH);
        }

        return content;
    }

    private Map<String, Object> buildMetadata(
            NotebookIntelligenceDocument notebook,
            String notebookId) {

        Map<String, Object> metadata = new HashMap<>();

        metadata.put("notebookId", notebookId);

        putIfText(metadata, "title", notebook.getTitle());
        putIfText(metadata, "status", notebook.getStatus());

        if (notebook.getTopics() != null
                && !notebook.getTopics().isEmpty()) {
            metadata.put("topics", notebook.getTopics());
        }

        if (notebook.getKeywords() != null
                && !notebook.getKeywords().isEmpty()) {
            metadata.put("keywords", notebook.getKeywords());
        }

        metadata.put("indexedAt", Instant.now().toString());

        return metadata;
    }

    private void addSection(
            List<String> sections,
            String label,
            String value) {

        if (StringUtils.hasText(value)) {
            sections.add(label + ": " + value.trim());
        }
    }

    private String join(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }

        return values.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
    }

    private void putIfText(
            Map<String, Object> metadata,
            String key,
            String value) {

        if (StringUtils.hasText(value)) {
            metadata.put(key, value.trim());
        }
    }

    private String requireText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(
                    fieldName + " must not be blank");
        }

        return value.trim();
    }
}