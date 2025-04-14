package com.banenor.util;

import com.banenor.model.AbstractAxles;
import com.banenor.repository.HaugfjellMP1AxlesRepository;
import com.banenor.repository.HaugfjellMP1HeaderRepository;
import com.banenor.repository.HaugfjellMP3AxlesRepository;
import com.banenor.repository.HaugfjellMP3HeaderRepository;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@SuppressWarnings("unchecked")
public class RepositoryResolver {

    private final HaugfjellMP1HeaderRepository mp1HeaderRepository;
    private final HaugfjellMP3HeaderRepository mp3HeaderRepository;
    private final HaugfjellMP1AxlesRepository mp1AxlesRepository;
    private final HaugfjellMP3AxlesRepository mp3AxlesRepository;

    public RepositoryResolver(HaugfjellMP1HeaderRepository mp1HeaderRepository,
                              HaugfjellMP3HeaderRepository mp3HeaderRepository,
                              HaugfjellMP1AxlesRepository mp1AxlesRepository,
                              HaugfjellMP3AxlesRepository mp3AxlesRepository) {
        this.mp1HeaderRepository = mp1HeaderRepository;
        this.mp3HeaderRepository = mp3HeaderRepository;
        this.mp1AxlesRepository = mp1AxlesRepository;
        this.mp3AxlesRepository = mp3AxlesRepository;
    }

    /**
     * Resolves the appropriate concrete axles repository for the given train number.
     * If a header is found in the MP1 header table, returns the MP1 axles repository;
     * otherwise, if found in MP3, returns the MP3 axles repository.
     *
     * @param trainNo the train number.
     * @return a Mono emitting the corresponding repository as a subtype of R2dbcRepository.
     */
    public Mono<R2dbcRepository<? extends AbstractAxles, Integer>> resolveRepository(Integer trainNo) {
        Mono<R2dbcRepository<? extends AbstractAxles, Integer>> mp1RepoMono = Mono.defer(() ->
                mp1HeaderRepository.findById(trainNo)
                        .flatMap(header -> Mono.just((R2dbcRepository<? extends AbstractAxles, Integer>) mp1AxlesRepository))
        );
        Mono<R2dbcRepository<? extends AbstractAxles, Integer>> mp3RepoMono = Mono.defer(() ->
                mp3HeaderRepository.findById(trainNo)
                        .flatMap(header -> Mono.just((R2dbcRepository<? extends AbstractAxles, Integer>) mp3AxlesRepository))
        );
        return mp1RepoMono
                .switchIfEmpty(mp3RepoMono)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("No header found for train number: " + trainNo)));
    }
}
