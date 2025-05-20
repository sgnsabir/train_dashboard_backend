package com.banenor.util;

import com.banenor.model.AbstractAxles;
import com.banenor.repository.HaugfjellMP1AxlesRepository;
import com.banenor.repository.HaugfjellMP1HeaderRepository;
import com.banenor.repository.HaugfjellMP3AxlesRepository;
import com.banenor.repository.HaugfjellMP3HeaderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;

/**
 * Dynamically routes calls to the appropriate Axles repository
 * (MP1 vs MP3) based on the given train number.  If trainNo is null,
 * returns a proxy that supports:
 *   - findAll()
 *   - findAllByCreatedAtBetween(start, end)
 */
@Slf4j
@Component
@SuppressWarnings("unchecked")
public class RepositoryResolver {

    private final HaugfjellMP1HeaderRepository mp1HeaderRepository;
    private final HaugfjellMP3HeaderRepository mp3HeaderRepository;
    private final HaugfjellMP1AxlesRepository   mp1AxlesRepository;
    private final HaugfjellMP3AxlesRepository   mp3AxlesRepository;

    public RepositoryResolver(
            HaugfjellMP1HeaderRepository mp1HeaderRepository,
            HaugfjellMP3HeaderRepository mp3HeaderRepository,
            HaugfjellMP1AxlesRepository mp1AxlesRepository,
            HaugfjellMP3AxlesRepository mp3AxlesRepository
    ) {
        this.mp1HeaderRepository = mp1HeaderRepository;
        this.mp3HeaderRepository = mp3HeaderRepository;
        this.mp1AxlesRepository  = mp1AxlesRepository;
        this.mp3AxlesRepository  = mp3AxlesRepository;
    }

    /**
     * @param trainNo the train identifier, or null to merge both repos
     * @return a Mono that emits the correct Axles repository for the train,
     *         or a combined proxy if trainNo is null.
     */
    public Mono<R2dbcRepository<? extends AbstractAxles, Integer>> resolveRepository(Integer trainNo) {
        log.debug("Resolving repository for trainNo={}", trainNo);

        if (trainNo == null) {
            log.debug("No trainNo provided; creating combined proxy");
            R2dbcRepository<AbstractAxles, Integer> combined =
                    (R2dbcRepository<AbstractAxles, Integer>) Proxy.newProxyInstance(
                            R2dbcRepository.class.getClassLoader(),
                            new Class[]{R2dbcRepository.class},
                            new CombinedHandler()
                    );
            return Mono.just(combined);
        }

        // Try MP1 first
        Mono<R2dbcRepository<? extends AbstractAxles, Integer>> mp1Mono =
                mp1HeaderRepository.findById(trainNo)
                        .map(h -> {
                            log.debug("Using MP1 repository for trainNo={}", trainNo);
                            return (R2dbcRepository<? extends AbstractAxles, Integer>) mp1AxlesRepository;
                        });

        // Otherwise try MP3
        Mono<R2dbcRepository<? extends AbstractAxles, Integer>> mp3Mono =
                mp3HeaderRepository.findById(trainNo)
                        .map(h -> {
                            log.debug("Using MP3 repository for trainNo={}", trainNo);
                            return (R2dbcRepository<? extends AbstractAxles, Integer>) mp3AxlesRepository;
                        });

        // MP1 if present, else MP3, else error
        return mp1Mono
                .switchIfEmpty(mp3Mono)
                .switchIfEmpty(Mono.defer(() -> {
                    String err = "No header found for train number: " + trainNo;
                    log.error(err);
                    return Mono.error(new IllegalArgumentException(err));
                }));
    }

    /**
     * InvocationHandler for the combined (trainNo==null) proxy.
     * Supports:
     *   - findAll()
     *   - findAllByCreatedAtBetween(LocalDateTime start, LocalDateTime end)
     */
    private class CombinedHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String name = method.getName();

            // findAll()
            if ("findAll".equals(name) && (args == null || args.length == 0)) {
                log.debug("CombinedHandler.invoke: findAll()");
                return Flux.merge(
                        mp1AxlesRepository.findAll().cast(AbstractAxles.class),
                        mp3AxlesRepository.findAll().cast(AbstractAxles.class)
                );
            }

            // findAllByCreatedAtBetween(start, end)
            if ("findAllByCreatedAtBetween".equals(name)
                    && args != null
                    && args.length == 2
                    && args[0] instanceof LocalDateTime
                    && args[1] instanceof LocalDateTime) {

                LocalDateTime start = (LocalDateTime) args[0];
                LocalDateTime end   = (LocalDateTime) args[1];
                log.debug("CombinedHandler.invoke: findAllByCreatedAtBetween({}, {})", start, end);
                return Flux.merge(
                        mp1AxlesRepository.findAllByCreatedAtBetween(start, end).cast(AbstractAxles.class),
                        mp3AxlesRepository.findAllByCreatedAtBetween(start, end).cast(AbstractAxles.class)
                );
            }

            String msg = "Combined repository only supports findAll() and findAllByCreatedAtBetween(...); attempted: " + name;
            log.warn(msg);
            throw new UnsupportedOperationException(msg);
        }
    }
}
