
package com.agilesolutions.intelligence.processor;

import com.agilesolutions.intelligence.document.NotebookIntelligenceDocument;
import com.agilesolutions.intelligence.domain.NotebookEnrichmentResult;
import com.agilesolutions.intelligence.repository.NotebookIntelligenceRepository;
import com.agilesolutions.intelligence.service.NotebookAiEnrichmentService;
import com.agilesolutions.intelligence.service.NotebookVectorIndexService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotebookIntelligenceProcessorTest {

    @Mock
    private NotebookAiEnrichmentService aiEnrichmentService;

    @Mock
    private NotebookIntelligenceRepository repository;

    @Mock
    private NotebookVectorIndexService vectorIndexService;

    private NotebookIntelligenceProcessor processor;

    private String notebookId;

    @BeforeEach
    void setUp() {
        processor = new NotebookIntelligenceProcessor(
                aiEnrichmentService,
                repository,
                vectorIndexService
        );

        notebookId = UUID.randomUUID().toString();
    }

    @Test
    void shouldPersistAiEnrichmentBeforeIndexing() {
        // Given
        NotebookEnrichmentResult enrichment =
                new NotebookEnrichmentResult(
                        "A notebook about Kubernetes deployments.",
                        List.of("Kubernetes", "Cloud Native"),
                        List.of("Docker", "Helm"),
                        List.of(
                                new NotebookEnrichmentResult.ExtractedEntity(
                                        "Kubernetes", "TECHNOLOGY")
                        )
                );

        when(aiEnrichmentService.enrich(
                "Kubernetes deployment",
                "Deploying applications using Helm"))
                .thenReturn(enrichment);

        when(repository.findById(notebookId))
                .thenReturn(Optional.empty());

        when(repository.save(any(NotebookIntelligenceDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        processor.process(
                notebookId,
                "Kubernetes deployment",
                "Deploying applications using Helm"
        );

        // Then: MongoDB persistence and Qdrant indexing both happen.
        var captor = org.mockito.ArgumentCaptor.forClass(
                NotebookIntelligenceDocument.class);

        verify(repository).save(captor.capture());

        NotebookIntelligenceDocument saved = captor.getValue();

        assertThat(saved.getNotebookId()).isEqualTo(notebookId);
        assertThat(saved.getTitle()).isEqualTo("Kubernetes deployment");
        assertThat(saved.getSummary())
                .isEqualTo("A notebook about Kubernetes deployments.");
        assertThat(saved.getTopics())
                .containsExactly("Kubernetes", "Cloud Native");
        assertThat(saved.getKeywords())
                .containsExactly("Docker", "Helm");
        assertThat(saved.getStatus()).isEqualTo("COMPLETED");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getEnrichedAt()).isNotNull();

        InOrder order = inOrder(repository, vectorIndexService);
        order.verify(repository).save(any(NotebookIntelligenceDocument.class));
        order.verify(vectorIndexService).index(any(NotebookIntelligenceDocument.class));

        verify(aiEnrichmentService).enrich(
                "Kubernetes deployment",
                "Deploying applications using Helm"
        );
    }

    @Test
    void shouldPreserveMongoDocumentWhenQdrantIndexingFails() {
        // Given
        NotebookEnrichmentResult enrichment =
                new NotebookEnrichmentResult(
                        "A notebook about Kubernetes.",
                        List.of("Kubernetes"),
                        List.of("containers"),
                        List.of()
                );

        when(aiEnrichmentService.enrich("Kubernetes", "Containers"))
                .thenReturn(enrichment);

        when(repository.findById(notebookId))
                .thenReturn(Optional.empty());

        when(repository.save(any(NotebookIntelligenceDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        doThrow(new RuntimeException("Simulated Qdrant outage"))
                .when(vectorIndexService)
                .index(any(NotebookIntelligenceDocument.class));

        // When / Then
        assertThatThrownBy(() -> processor.process(
                notebookId, "Kubernetes", "Containers"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Simulated Qdrant outage");

        // MongoDB persistence succeeded before Qdrant failed.
        verify(repository).save(any(NotebookIntelligenceDocument.class));
        verify(vectorIndexService).index(any(NotebookIntelligenceDocument.class));
    }

    @Test
    void shouldRetryVectorIndexingWithoutRepeatingAiEnrichment() {
        // Given: an already enriched document exists in MongoDB.
        NotebookIntelligenceDocument existing =
                new NotebookIntelligenceDocument();

        existing.setNotebookId(notebookId);
        existing.setTitle("Kubernetes");
        existing.setDescription("Containers");
        existing.setSummary("A notebook about Kubernetes.");
        existing.setTopics(List.of("Kubernetes"));
        existing.setKeywords(List.of("containers"));
        existing.setStatus("COMPLETED");
        existing.setCreatedAt(Instant.now());
        existing.setUpdatedAt(Instant.now());
        existing.setEnrichedAt(Instant.now());

        when(repository.findById(notebookId))
                .thenReturn(Optional.of(existing));

        doThrow(new RuntimeException("Simulated Qdrant outage"))
                .doNothing()
                .when(vectorIndexService)
                .index(existing);

        // First attempt fails.
        assertThatThrownBy(() -> processor.retryVectorIndexing(notebookId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Simulated Qdrant outage");

        // Second attempt succeeds.
        processor.retryVectorIndexing(notebookId);

        // AI is not called again; MongoDB isn't rewritten.
        verify(aiEnrichmentService, never())
                .enrich(anyString(), any());

        verify(repository, never())
                .save(any(NotebookIntelligenceDocument.class));

        verify(repository, times(2)).findById(notebookId);
        verify(vectorIndexService, times(2)).index(existing);
    }
}