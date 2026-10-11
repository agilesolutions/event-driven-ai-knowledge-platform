package com.agilesolutions.intelligence.init;

import com.agilesolutions.intelligence.document.NotebookIntelligenceDocument;
import com.agilesolutions.intelligence.service.NotebookVectorIndexService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QdrantDataLoaderTest {

    @Mock
    private NotebookVectorIndexService vectorIndexService;

    @Mock
    private ResourceLoader resourceLoader;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ApplicationArguments args;

    @Mock
    private Resource resource;

    private QdrantDataLoader loader;

    @BeforeEach
    void setUp() {
        loader = new QdrantDataLoader(vectorIndexService, resourceLoader, objectMapper);
    }

    @Test
    void shouldReturnEarlyWhenSkipFlagPresent() throws Exception {
        when(args.containsOption("skip-vector-seed")).thenReturn(true);

        loader.run(args);

        verifyNoInteractions(resourceLoader);
        verifyNoInteractions(objectMapper);
        verifyNoInteractions(vectorIndexService);
    }

    @Test
    void shouldAbortWhenResourceMissing() throws Exception {
        when(args.containsOption("skip-vector-seed")).thenReturn(false);
        when(resourceLoader.getResource("classpath:data/notebook-seed.json")).thenReturn(resource);
        when(resource.exists()).thenReturn(false);

        loader.run(args);

        verify(resourceLoader).getResource("classpath:data/notebook-seed.json");
        verify(resource).exists();
        verifyNoInteractions(objectMapper);
        verifyNoInteractions(vectorIndexService);
    }

    @Test
    void shouldIndexDocumentsAndContinueOnIllegalArgument() throws Exception {
        when(args.containsOption("skip-vector-seed")).thenReturn(false);
        when(resourceLoader.getResource("classpath:data/notebook-seed.json")).thenReturn(resource);
        when(resource.exists()).thenReturn(true);

        // Provide a dummy InputStream (not actually consumed because objectMapper is mocked)
        InputStream dummyStream = mock(InputStream.class);
        when(resource.getInputStream()).thenReturn(dummyStream);

        NotebookIntelligenceDocument good = new NotebookIntelligenceDocument();
        good.setNotebookId("id-1");
        good.setTitle("Good");

        NotebookIntelligenceDocument bad = new NotebookIntelligenceDocument();
        bad.setNotebookId("id-2");
        bad.setTitle("Bad");

        List<NotebookIntelligenceDocument> docs = List.of(good, bad);

        // Mock ObjectMapper to return our prepared list regardless of the TypeReference
        when(objectMapper.readValue(any(InputStream.class), any(TypeReference.class))).thenReturn(docs);

        // Simulate vector indexing throwing IllegalArgumentException for the bad document
        doNothing().when(vectorIndexService).index(good);
        doThrow(new IllegalArgumentException("invalid vector payload")).when(vectorIndexService).index(bad);

        // Should not throw despite the IllegalArgumentException on one document
        loader.run(args);

        ArgumentCaptor<NotebookIntelligenceDocument> captor = ArgumentCaptor.forClass(NotebookIntelligenceDocument.class);
        verify(vectorIndexService, times(2)).index(captor.capture());

        List<NotebookIntelligenceDocument> indexed = captor.getAllValues();
        assertThat(indexed).hasSize(2);
        assertThat(indexed.get(0).getNotebookId()).isEqualTo("id-1");
        assertThat(indexed.get(1).getNotebookId()).isEqualTo("id-2");

        verify(resource).getInputStream();
        verify(objectMapper).readValue(any(InputStream.class), any(TypeReference.class));
    }

    @Test
    void shouldLoadRealJsonAndIndex() throws Exception {
        // Use real resource loader and object mapper to read the bundled JSON fixture
        ResourceLoader realLoader = new DefaultResourceLoader();
        ObjectMapper realMapper = new ObjectMapper();
        // Enable Java time module so Instant fields deserialize from ISO-8601 strings
        realMapper.registerModule(new JavaTimeModule());
        realMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        QdrantDataLoader realLoaderBean = new QdrantDataLoader(vectorIndexService, realLoader, realMapper);

        when(args.containsOption("skip-vector-seed")).thenReturn(false);

        // Execute against the real JSON file in src/main/resources/data/notebook-seed.json
        realLoaderBean.run(args);

        ArgumentCaptor<NotebookIntelligenceDocument> captor = ArgumentCaptor.forClass(NotebookIntelligenceDocument.class);
        verify(vectorIndexService, atLeastOnce()).index(captor.capture());

        List<NotebookIntelligenceDocument> captured = captor.getAllValues();
        assertThat(captured).isNotEmpty();

        // Validate the known record in the seed file is present
        boolean found = captured.stream().anyMatch(d -> "notebook-001".equals(d.getNotebookId()));
        assertThat(found).isTrue();
    }
}
