package com.banenor.util;

import com.banenor.model.HaugfjellMP1Header;
import com.banenor.model.HaugfjellMP3Header;
import com.banenor.repository.HaugfjellMP1AxlesRepository;
import com.banenor.repository.HaugfjellMP1HeaderRepository;
import com.banenor.repository.HaugfjellMP3AxlesRepository;
import com.banenor.repository.HaugfjellMP3HeaderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@TestPropertySource(locations = "classpath:test.properties")
@DisplayName("RepositoryResolver Tests")
class RepositoryResolverTest {

    private HaugfjellMP1HeaderRepository mp1HeaderRepository;
    private HaugfjellMP3HeaderRepository mp3HeaderRepository;
    private HaugfjellMP1AxlesRepository mp1AxlesRepository;
    private HaugfjellMP3AxlesRepository mp3AxlesRepository;
    private RepositoryResolver repositoryResolver;

    @BeforeEach
    void setUp() {
        mp1HeaderRepository = mock(HaugfjellMP1HeaderRepository.class);
        mp3HeaderRepository = mock(HaugfjellMP3HeaderRepository.class);
        mp1AxlesRepository = mock(HaugfjellMP1AxlesRepository.class);
        mp3AxlesRepository = mock(HaugfjellMP3AxlesRepository.class);
        repositoryResolver = new RepositoryResolver(mp1HeaderRepository, mp3HeaderRepository, mp1AxlesRepository, mp3AxlesRepository);
    }

    @Test
    @DisplayName("Should resolve repository for MP1 header")
    void testResolveRepositoryForMP1() {
        final int trainNo = 1;
        HaugfjellMP1Header header = new HaugfjellMP1Header();
        when(mp1HeaderRepository.findById(trainNo)).thenReturn(Mono.just(header));

        StepVerifier.create(repositoryResolver.resolveRepository(trainNo))
                .assertNext(resolvedRepo -> {
                    assertNotNull(resolvedRepo, "Resolved repository should not be null for MP1 header");
                    assertEquals(mp1AxlesRepository, resolvedRepo, "Resolved repository should be MP1AxlesRepository for trainNo: " + trainNo);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should resolve repository for MP3 header when MP1 header is absent")
    void testResolveRepositoryForMP3() {
        final int trainNo = 2;
        when(mp1HeaderRepository.findById(trainNo)).thenReturn(Mono.empty());
        HaugfjellMP3Header header = new HaugfjellMP3Header();
        when(mp3HeaderRepository.findById(trainNo)).thenReturn(Mono.just(header));

        StepVerifier.create(repositoryResolver.resolveRepository(trainNo))
                .assertNext(resolvedRepo -> {
                    assertNotNull(resolvedRepo, "Resolved repository should not be null for MP3 header");
                    assertEquals(mp3AxlesRepository, resolvedRepo, "Resolved repository should be MP3AxlesRepository for trainNo: " + trainNo);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when no header is found")
    void testResolveRepositoryThrowsExceptionWhenNoHeaderFound() {
        final int trainNo = 999;
        when(mp1HeaderRepository.findById(trainNo)).thenReturn(Mono.empty());
        when(mp3HeaderRepository.findById(trainNo)).thenReturn(Mono.empty());

        StepVerifier.create(repositoryResolver.resolveRepository(trainNo))
                .expectErrorMatches(throwable -> throwable instanceof IllegalArgumentException &&
                        throwable.getMessage().contains("No header found for train number"))
                .verify();
    }
}
