package com.banenor.util;

import com.banenor.model.AbstractAxles;
import com.banenor.repository.HaugfjellMP1AxlesRepository;
import com.banenor.repository.HaugfjellMP1HeaderRepository;
import com.banenor.repository.HaugfjellMP3AxlesRepository;
import com.banenor.repository.HaugfjellMP3HeaderRepository;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

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

    public Mono<R2dbcRepository<? extends AbstractAxles, Integer>> resolveRepository(Integer trainNo) {
        if (trainNo == null) {
            // build a proxy that only supports findAll()
            R2dbcRepository<AbstractAxles, Integer> combinedRepo =
                    (R2dbcRepository<AbstractAxles, Integer>) Proxy.newProxyInstance(
                            R2dbcRepository.class.getClassLoader(),
                            new Class[]{R2dbcRepository.class},
                            new InvocationHandler() {
                                @Override
                                public Object invoke(Object proxy, Method method, Object[] args) {
                                    // only merge both repos on findAll()
                                    if ("findAll".equals(method.getName()) && (args == null || args.length == 0)) {
                                        return Flux.merge(
                                                mp1AxlesRepository.findAll().cast(AbstractAxles.class),
                                                mp3AxlesRepository.findAll().cast(AbstractAxles.class)
                                        );
                                    }
                                    throw new UnsupportedOperationException(
                                            "Combined repository only supports findAll(); attempted: " + method.getName()
                                    );
                                }
                            }
                    );

            return Mono.just(combinedRepo);
        }

        Mono<R2dbcRepository<? extends AbstractAxles, Integer>> mp1Lookup =
                Mono.defer(() ->
                        mp1HeaderRepository.findById(trainNo)
                                .map(_h -> (R2dbcRepository<? extends AbstractAxles, Integer>) mp1AxlesRepository)
                );
        Mono<R2dbcRepository<? extends AbstractAxles, Integer>> mp3Lookup =
                Mono.defer(() ->
                        mp3HeaderRepository.findById(trainNo)
                                .map(_h -> (R2dbcRepository<? extends AbstractAxles, Integer>) mp3AxlesRepository)
                );

        return mp1Lookup
                .switchIfEmpty(mp3Lookup)
                .switchIfEmpty(Mono.error(
                        new IllegalArgumentException("No header found for train number: " + trainNo)
                ));
    }
}
