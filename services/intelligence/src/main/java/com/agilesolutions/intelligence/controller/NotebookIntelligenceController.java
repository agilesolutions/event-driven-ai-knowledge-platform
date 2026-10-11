
package com.agilesolutions.intelligence.controller;



import com.agilesolutions.intelligence.document.NotebookIntelligenceDocument;
import com.agilesolutions.intelligence.service.NotebookIntelligenceService;

import com.agilesolutions.notebook.api.NotebookIntelligenceApi;
import com.agilesolutions.notebook.api.model.ExtractedEntity;
import com.agilesolutions.notebook.api.model.IntelligenceSourceEvent;
import com.agilesolutions.notebook.api.model.NotebookIntelligence;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class NotebookIntelligenceController
        implements NotebookIntelligenceApi {

    private final NotebookIntelligenceService service;


    @Override
    public ResponseEntity<List<NotebookIntelligence>>
    searchNotebookIntelligence(String q) {

        List<NotebookIntelligence> results = service.search(q)
                .stream()
                .map(this::toApiModel)
                .toList();

        return ResponseEntity.ok(results);
    }

    @Override
    public ResponseEntity<NotebookIntelligence>
    getNotebookIntelligenceById(UUID id) {

        return ResponseEntity.ok(
                toApiModel(service.getById(id.toString()))
        );
    }

    @Override
    public ResponseEntity<List<NotebookIntelligence>>
    getNotebooksByTopic(String topic) {

        List<NotebookIntelligence> results =
                service.getByTopic(topic)
                        .stream()
                        .map(this::toApiModel)
                        .toList();

        return ResponseEntity.ok(results);
    }

    private NotebookIntelligence toApiModel(
            NotebookIntelligenceDocument document) {

        NotebookIntelligence result = new NotebookIntelligence();

        result.setNotebookId(
                UUID.fromString(document.getNotebookId())
        );
        result.setTitle(document.getTitle());
        result.setDescription(document.getDescription());

        // The OpenAPI schema defines status as a string enum.
        result.setStatus(
                NotebookIntelligence.StatusEnum.valueOf(
                        document.getStatus()
                )
        );

        result.setTopics(document.getTopics());
        result.setKeywords(document.getKeywords());

        List<ExtractedEntity> entities =
                document.getEntities()
                        .stream()
                        .map(entity -> {
                            ExtractedEntity dto = new ExtractedEntity();
                            dto.setName(entity.getName());
                            dto.setType(entity.getType());
                            return dto;
                        })
                        .toList();

        result.setEntities(entities);
        result.setSummary(document.getSummary());

        if (document.getSource() != null) {
            var source = document.getSource();
            IntelligenceSourceEvent sourceDto =
                    new IntelligenceSourceEvent();

            sourceDto.setEventId(source.getEventId());
            sourceDto.setEventType(source.getEventType());
            sourceDto.setTopic(source.getTopic());
            sourceDto.setPartition(source.getPartition());
            sourceDto.setOffset(source.getOffset());
            sourceDto.setSchemaVersion(source.getSchemaVersion());

            result.setSource(sourceDto);
        }

        result.setCreatedAt(toOffsetDateTime(document.getCreatedAt()));
        result.setUpdatedAt(toOffsetDateTime(document.getUpdatedAt()));
        result.setEnrichedAt(toOffsetDateTime(document.getEnrichedAt()));

        return result;
    }

    private OffsetDateTime toOffsetDateTime(Instant value) {
        return value == null
                ? null
                : value.atOffset(ZoneOffset.UTC);
    }
}