package com.banenor.service;

import com.banenor.repository.HaugfjellMP1AxlesRepository;
import com.banenor.repository.HaugfjellMP3AxlesRepository;
import com.banenor.util.RepositoryResolver;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class AggregationServiceImpl implements AggregationService {

    private final RepositoryResolver repositoryResolver;

    public AggregationServiceImpl(RepositoryResolver repositoryResolver) {
        this.repositoryResolver = repositoryResolver;
    }

    /**
     * Helper method that resolves the repository for the given train number and applies the provided query function.
     *
     * @param trainNo       the train number
     * @param queryFunction a function that takes the resolved repository and returns a Mono of the desired value
     * @param <T>           the type of the result
     * @return a Mono emitting the result of the query function
     */
    private <T> Mono<T> executeQuery(Integer trainNo, java.util.function.Function<Object, Mono<T>> queryFunction) {
        return repositoryResolver.resolveRepository(trainNo)
                .flatMap(repo -> queryFunction.apply(repo));
    }

    // --- Speed Aggregations ---
    @Override
    public Mono<Double> getAverageSpeed(Integer trainNo) {
        return executeQuery(trainNo, repo -> {
            if (repo instanceof HaugfjellMP1AxlesRepository) {
                return ((HaugfjellMP1AxlesRepository) repo).findAverageSpeedByTrainNo(trainNo);
            } else if (repo instanceof HaugfjellMP3AxlesRepository) {
                return ((HaugfjellMP3AxlesRepository) repo).findAverageSpeedByTrainNo(trainNo);
            } else {
                return Mono.error(new IllegalStateException("Unsupported repository type"));
            }
        });
    }

    @Override
    public Mono<Double> getMinSpeed(Integer trainNo) {
        return executeQuery(trainNo, repo -> {
            if (repo instanceof HaugfjellMP1AxlesRepository) {
                return ((HaugfjellMP1AxlesRepository) repo).findMinSpeedByTrainNo(trainNo);
            } else if (repo instanceof HaugfjellMP3AxlesRepository) {
                return ((HaugfjellMP3AxlesRepository) repo).findMinSpeedByTrainNo(trainNo);
            } else {
                return Mono.error(new IllegalStateException("Unsupported repository type"));
            }
        });
    }

    @Override
    public Mono<Double> getMaxSpeed(Integer trainNo) {
        return executeQuery(trainNo, repo -> {
            if (repo instanceof HaugfjellMP1AxlesRepository) {
                return ((HaugfjellMP1AxlesRepository) repo).findMaxSpeedByTrainNo(trainNo);
            } else if (repo instanceof HaugfjellMP3AxlesRepository) {
                return ((HaugfjellMP3AxlesRepository) repo).findMaxSpeedByTrainNo(trainNo);
            } else {
                return Mono.error(new IllegalStateException("Unsupported repository type"));
            }
        });
    }

    @Override
    public Mono<Double> getSpeedVariance(Integer trainNo) {
        return executeQuery(trainNo, repo -> {
            Mono<Double> avgSq;
            Mono<Double> avg;
            if (repo instanceof HaugfjellMP1AxlesRepository) {
                avgSq = ((HaugfjellMP1AxlesRepository) repo).findAverageSquareSpeedByTrainNo(trainNo);
                avg = ((HaugfjellMP1AxlesRepository) repo).findAverageSpeedByTrainNo(trainNo);
            } else if (repo instanceof HaugfjellMP3AxlesRepository) {
                avgSq = ((HaugfjellMP3AxlesRepository) repo).findAverageSquareSpeedByTrainNo(trainNo);
                avg = ((HaugfjellMP3AxlesRepository) repo).findAverageSpeedByTrainNo(trainNo);
            } else {
                return Mono.error(new IllegalStateException("Unsupported repository type"));
            }
            return avgSq.zipWith(avg, (a, b) -> (a != null && b != null) ? a - (b * b) : 0.0);
        });
    }

    // --- Angle of Attack Aggregations ---
    @Override
    public Mono<Double> getAverageAoa(Integer trainNo) {
        return executeQuery(trainNo, repo -> {
            if (repo instanceof HaugfjellMP1AxlesRepository) {
                return ((HaugfjellMP1AxlesRepository) repo).findAverageAoaByTrainNo(trainNo);
            } else if (repo instanceof HaugfjellMP3AxlesRepository) {
                return ((HaugfjellMP3AxlesRepository) repo).findAverageAoaByTrainNo(trainNo);
            } else {
                return Mono.error(new IllegalStateException("Unsupported repository type"));
            }
        });
    }

    @Override
    public Mono<Double> getMinAoa(Integer trainNo) {
        return executeQuery(trainNo, repo -> {
            if (repo instanceof HaugfjellMP1AxlesRepository) {
                return ((HaugfjellMP1AxlesRepository) repo).findMinAoaByTrainNo(trainNo);
            } else if (repo instanceof HaugfjellMP3AxlesRepository) {
                return ((HaugfjellMP3AxlesRepository) repo).findMinAoaByTrainNo(trainNo);
            } else {
                return Mono.error(new IllegalStateException("Unsupported repository type"));
            }
        });
    }

    @Override
    public Mono<Double> getMaxAoa(Integer trainNo) {
        return executeQuery(trainNo, repo -> {
            if (repo instanceof HaugfjellMP1AxlesRepository) {
                return ((HaugfjellMP1AxlesRepository) repo).findMaxAoaByTrainNo(trainNo);
            } else if (repo instanceof HaugfjellMP3AxlesRepository) {
                return ((HaugfjellMP3AxlesRepository) repo).findMaxAoaByTrainNo(trainNo);
            } else {
                return Mono.error(new IllegalStateException("Unsupported repository type"));
            }
        });
    }

    @Override
    public Mono<Double> getAoaVariance(Integer trainNo) {
        return executeQuery(trainNo, repo -> {
            Mono<Double> avgSq;
            Mono<Double> avg;
            if (repo instanceof HaugfjellMP1AxlesRepository) {
                avgSq = ((HaugfjellMP1AxlesRepository) repo).findAverageSquareAoaByTrainNo(trainNo);
                avg = ((HaugfjellMP1AxlesRepository) repo).findAverageAoaByTrainNo(trainNo);
            } else if (repo instanceof HaugfjellMP3AxlesRepository) {
                avgSq = ((HaugfjellMP3AxlesRepository) repo).findAverageSquareAoaByTrainNo(trainNo);
                avg = ((HaugfjellMP3AxlesRepository) repo).findAverageAoaByTrainNo(trainNo);
            } else {
                return Mono.error(new IllegalStateException("Unsupported repository type"));
            }
            return avgSq.zipWith(avg, (a, b) -> (a != null && b != null) ? a - (b * b) : 0.0);
        });
    }

    // --- Vibration Aggregations (Left) ---
    @Override
    public Mono<Double> getAverageVibrationLeft(Integer trainNo) {
        return executeQuery(trainNo, repo -> {
            if (repo instanceof HaugfjellMP1AxlesRepository) {
                return ((HaugfjellMP1AxlesRepository) repo).findAverageVibrationLeftByTrainNo(trainNo);
            } else if (repo instanceof HaugfjellMP3AxlesRepository) {
                return ((HaugfjellMP3AxlesRepository) repo).findAverageVibrationLeftByTrainNo(trainNo);
            } else {
                return Mono.error(new IllegalStateException("Unsupported repository type"));
            }
        });
    }

    @Override
    public Mono<Double> getMinVibrationLeft(Integer trainNo) {
        return executeQuery(trainNo, repo -> {
            if (repo instanceof HaugfjellMP1AxlesRepository) {
                return ((HaugfjellMP1AxlesRepository) repo).findMinVibrationLeftByTrainNo(trainNo);
            } else if (repo instanceof HaugfjellMP3AxlesRepository) {
                return ((HaugfjellMP3AxlesRepository) repo).findMinVibrationLeftByTrainNo(trainNo);
            } else {
                return Mono.error(new IllegalStateException("Unsupported repository type"));
            }
        });
    }

    @Override
    public Mono<Double> getMaxVibrationLeft(Integer trainNo) {
        return executeQuery(trainNo, repo -> {
            if (repo instanceof HaugfjellMP1AxlesRepository) {
                return ((HaugfjellMP1AxlesRepository) repo).findMaxVibrationLeftByTrainNo(trainNo);
            } else if (repo instanceof HaugfjellMP3AxlesRepository) {
                return ((HaugfjellMP3AxlesRepository) repo).findMaxVibrationLeftByTrainNo(trainNo);
            } else {
                return Mono.error(new IllegalStateException("Unsupported repository type"));
            }
        });
    }

    @Override
    public Mono<Double> getVibrationLeftVariance(Integer trainNo) {
        return executeQuery(trainNo, repo -> {
            Mono<Double> avgSq;
            Mono<Double> avg;
            if (repo instanceof HaugfjellMP1AxlesRepository) {
                avgSq = ((HaugfjellMP1AxlesRepository) repo).findAverageSquareVibrationLeftByTrainNo(trainNo);
                avg = ((HaugfjellMP1AxlesRepository) repo).findAverageVibrationLeftByTrainNo(trainNo);
            } else if (repo instanceof HaugfjellMP3AxlesRepository) {
                avgSq = ((HaugfjellMP3AxlesRepository) repo).findAverageSquareVibrationLeftByTrainNo(trainNo);
                avg = ((HaugfjellMP3AxlesRepository) repo).findAverageVibrationLeftByTrainNo(trainNo);
            } else {
                return Mono.error(new IllegalStateException("Unsupported repository type"));
            }
            return avgSq.zipWith(avg, (a, b) -> (a != null && b != null) ? a - (b * b) : 0.0);
        });
    }

    // --- Vibration Aggregations (Right) ---
    @Override
    public Mono<Double> getAverageVibrationRight(Integer trainNo) {
        return executeQuery(trainNo, repo -> {
            if (repo instanceof HaugfjellMP1AxlesRepository) {
                return ((HaugfjellMP1AxlesRepository) repo).findAverageVibrationRightByTrainNo(trainNo);
            } else if (repo instanceof HaugfjellMP3AxlesRepository) {
                return ((HaugfjellMP3AxlesRepository) repo).findAverageVibrationRightByTrainNo(trainNo);
            } else {
                return Mono.error(new IllegalStateException("Unsupported repository type"));
            }
        });
    }

    @Override
    public Mono<Double> getMinVibrationRight(Integer trainNo) {
        return executeQuery(trainNo, repo -> {
            if (repo instanceof HaugfjellMP1AxlesRepository) {
                return ((HaugfjellMP1AxlesRepository) repo).findMinVibrationRightByTrainNo(trainNo);
            } else if (repo instanceof HaugfjellMP3AxlesRepository) {
                return ((HaugfjellMP3AxlesRepository) repo).findMinVibrationRightByTrainNo(trainNo);
            } else {
                return Mono.error(new IllegalStateException("Unsupported repository type"));
            }
        });
    }

    @Override
    public Mono<Double> getMaxVibrationRight(Integer trainNo) {
        return executeQuery(trainNo, repo -> {
            if (repo instanceof HaugfjellMP1AxlesRepository) {
                return ((HaugfjellMP1AxlesRepository) repo).findMaxVibrationRightByTrainNo(trainNo);
            } else if (repo instanceof HaugfjellMP3AxlesRepository) {
                return ((HaugfjellMP3AxlesRepository) repo).findMaxVibrationRightByTrainNo(trainNo);
            } else {
                return Mono.error(new IllegalStateException("Unsupported repository type"));
            }
        });
    }

    @Override
    public Mono<Double> getVibrationRightVariance(Integer trainNo) {
        return executeQuery(trainNo, repo -> {
            Mono<Double> avgSq;
            Mono<Double> avg;
            if (repo instanceof HaugfjellMP1AxlesRepository) {
                avgSq = ((HaugfjellMP1AxlesRepository) repo).findAverageSquareVibrationRightByTrainNo(trainNo);
                avg = ((HaugfjellMP1AxlesRepository) repo).findAverageVibrationRightByTrainNo(trainNo);
            } else if (repo instanceof HaugfjellMP3AxlesRepository) {
                avgSq = ((HaugfjellMP3AxlesRepository) repo).findAverageSquareVibrationRightByTrainNo(trainNo);
                avg = ((HaugfjellMP3AxlesRepository) repo).findAverageVibrationRightByTrainNo(trainNo);
            } else {
                return Mono.error(new IllegalStateException("Unsupported repository type"));
            }
            return avgSq.zipWith(avg, (a, b) -> (a != null && b != null) ? a - (b * b) : 0.0);
        });
    }

    // --- Vertical Force Aggregations (Left) ---
    @Override
    public Mono<Double> getAverageVerticalForceLeft(Integer trainNo) {
        return executeQuery(trainNo, repo -> {
            if (repo instanceof HaugfjellMP1AxlesRepository) {
                return ((HaugfjellMP1AxlesRepository) repo).findAverageVerticalForceLeftByTrainNo(trainNo);
            } else if (repo instanceof HaugfjellMP3AxlesRepository) {
                return ((HaugfjellMP3AxlesRepository) repo).findAverageVerticalForceLeftByTrainNo(trainNo);
            } else {
                return Mono.error(new IllegalStateException("Unsupported repository type"));
            }
        });
    }

    @Override
    public Mono<Double> getMinVerticalForceLeft(Integer trainNo) {
        return executeQuery(trainNo, repo -> {
            if (repo instanceof HaugfjellMP1AxlesRepository) {
                return ((HaugfjellMP1AxlesRepository) repo).findMinVerticalForceLeftByTrainNo(trainNo);
            } else if (repo instanceof HaugfjellMP3AxlesRepository) {
                return ((HaugfjellMP3AxlesRepository) repo).findMinVerticalForceLeftByTrainNo(trainNo);
            } else {
                return Mono.error(new IllegalStateException("Unsupported repository type"));
            }
        });
    }

    @Override
    public Mono<Double> getMaxVerticalForceLeft(Integer trainNo) {
        return executeQuery(trainNo, repo -> {
            if (repo instanceof HaugfjellMP1AxlesRepository) {
                return ((HaugfjellMP1AxlesRepository) repo).findMaxVerticalForceLeftByTrainNo(trainNo);
            } else if (repo instanceof HaugfjellMP3AxlesRepository) {
                return ((HaugfjellMP3AxlesRepository) repo).findMaxVerticalForceLeftByTrainNo(trainNo);
            } else {
                return Mono.error(new IllegalStateException("Unsupported repository type"));
            }
        });
    }

    @Override
    public Mono<Double> getVerticalForceLeftVariance(Integer trainNo) {
        return executeQuery(trainNo, repo -> {
            Mono<Double> avgSq;
            Mono<Double> avg;
            if (repo instanceof HaugfjellMP1AxlesRepository) {
                avgSq = ((HaugfjellMP1AxlesRepository) repo).findAverageSquareVerticalForceLeftByTrainNo(trainNo);
                avg = ((HaugfjellMP1AxlesRepository) repo).findAverageVerticalForceLeftByTrainNo(trainNo);
            } else if (repo instanceof HaugfjellMP3AxlesRepository) {
                avgSq = ((HaugfjellMP3AxlesRepository) repo).findAverageSquareVerticalForceLeftByTrainNo(trainNo);
                avg = ((HaugfjellMP3AxlesRepository) repo).findAverageVerticalForceLeftByTrainNo(trainNo);
            } else {
                return Mono.error(new IllegalStateException("Unsupported repository type"));
            }
            return avgSq.zipWith(avg, (a, b) -> (a != null && b != null) ? a - (b * b) : 0.0);
        });
    }

    // --- Vertical Force Aggregations (Right) ---
    @Override
    public Mono<Double> getAverageVerticalForceRight(Integer trainNo) {
        return executeQuery(trainNo, repo -> {
            if (repo instanceof HaugfjellMP1AxlesRepository) {
                return ((HaugfjellMP1AxlesRepository) repo).findAverageVerticalForceRightByTrainNo(trainNo);
            } else if (repo instanceof HaugfjellMP3AxlesRepository) {
                return ((HaugfjellMP3AxlesRepository) repo).findAverageVerticalForceRightByTrainNo(trainNo);
            } else {
                return Mono.error(new IllegalStateException("Unsupported repository type"));
            }
        });
    }

    @Override
    public Mono<Double> getMinVerticalForceRight(Integer trainNo) {
        return executeQuery(trainNo, repo -> {
            if (repo instanceof HaugfjellMP1AxlesRepository) {
                return ((HaugfjellMP1AxlesRepository) repo).findMinVerticalForceRightByTrainNo(trainNo);
            } else if (repo instanceof HaugfjellMP3AxlesRepository) {
                return ((HaugfjellMP3AxlesRepository) repo).findMinVerticalForceRightByTrainNo(trainNo);
            } else {
                return Mono.error(new IllegalStateException("Unsupported repository type"));
            }
        });
    }

    @Override
    public Mono<Double> getMaxVerticalForceRight(Integer trainNo) {
        return executeQuery(trainNo, repo -> {
            if (repo instanceof HaugfjellMP1AxlesRepository) {
                return ((HaugfjellMP1AxlesRepository) repo).findMaxVerticalForceRightByTrainNo(trainNo);
            } else if (repo instanceof HaugfjellMP3AxlesRepository) {
                return ((HaugfjellMP3AxlesRepository) repo).findMaxVerticalForceRightByTrainNo(trainNo);
            } else {
                return Mono.error(new IllegalStateException("Unsupported repository type"));
            }
        });
    }

    @Override
    public Mono<Double> getVerticalForceRightVariance(Integer trainNo) {
        return executeQuery(trainNo, repo -> {
            Mono<Double> avgSq;
            Mono<Double> avg;
            if (repo instanceof HaugfjellMP1AxlesRepository) {
                avgSq = ((HaugfjellMP1AxlesRepository) repo).findAverageSquareVerticalForceRightByTrainNo(trainNo);
                avg = ((HaugfjellMP1AxlesRepository) repo).findAverageVerticalForceRightByTrainNo(trainNo);
            } else if (repo instanceof HaugfjellMP3AxlesRepository) {
                avgSq = ((HaugfjellMP3AxlesRepository) repo).findAverageSquareVerticalForceRightByTrainNo(trainNo);
                avg = ((HaugfjellMP3AxlesRepository) repo).findAverageVerticalForceRightByTrainNo(trainNo);
            } else {
                return Mono.error(new IllegalStateException("Unsupported repository type"));
            }
            return avgSq.zipWith(avg, (a, b) -> (a != null && b != null) ? a - (b * b) : 0.0);
        });
    }

    // --- Lateral Force Aggregations (Left) ---
    @Override
    public Mono<Double> getAverageLateralForceLeft(Integer trainNo) {
        return executeQuery(trainNo, repo -> {
            if (repo instanceof HaugfjellMP1AxlesRepository) {
                return ((HaugfjellMP1AxlesRepository) repo).findAverageLateralForceLeftByTrainNo(trainNo);
            } else if (repo instanceof HaugfjellMP3AxlesRepository) {
                return ((HaugfjellMP3AxlesRepository) repo).findAverageLateralForceLeftByTrainNo(trainNo);
            } else {
                return Mono.error(new IllegalStateException("Unsupported repository type"));
            }
        });
    }

    @Override
    public Mono<Double> getMinLateralForceLeft(Integer trainNo) {
        return executeQuery(trainNo, repo -> {
            if (repo instanceof HaugfjellMP1AxlesRepository) {
                return ((HaugfjellMP1AxlesRepository) repo).findMinLateralForceLeftByTrainNo(trainNo);
            } else if (repo instanceof HaugfjellMP3AxlesRepository) {
                return ((HaugfjellMP3AxlesRepository) repo).findMinLateralForceLeftByTrainNo(trainNo);
            } else {
                return Mono.error(new IllegalStateException("Unsupported repository type"));
            }
        });
    }

    @Override
    public Mono<Double> getMaxLateralForceLeft(Integer trainNo) {
        return executeQuery(trainNo, repo -> {
            if (repo instanceof HaugfjellMP1AxlesRepository) {
                return ((HaugfjellMP1AxlesRepository) repo).findMaxLateralForceLeftByTrainNo(trainNo);
            } else if (repo instanceof HaugfjellMP3AxlesRepository) {
                return ((HaugfjellMP3AxlesRepository) repo).findMaxLateralForceLeftByTrainNo(trainNo);
            } else {
                return Mono.error(new IllegalStateException("Unsupported repository type"));
            }
        });
    }

    @Override
    public Mono<Double> getLateralForceLeftVariance(Integer trainNo) {
        return executeQuery(trainNo, repo -> {
            Mono<Double> avgSq;
            Mono<Double> avg;
            if (repo instanceof HaugfjellMP1AxlesRepository) {
                avgSq = ((HaugfjellMP1AxlesRepository) repo).findAverageSquareLateralForceLeftByTrainNo(trainNo);
                avg = ((HaugfjellMP1AxlesRepository) repo).findAverageLateralForceLeftByTrainNo(trainNo);
            } else if (repo instanceof HaugfjellMP3AxlesRepository) {
                avgSq = ((HaugfjellMP3AxlesRepository) repo).findAverageSquareLateralForceLeftByTrainNo(trainNo);
                avg = ((HaugfjellMP3AxlesRepository) repo).findAverageLateralForceLeftByTrainNo(trainNo);
            } else {
                return Mono.error(new IllegalStateException("Unsupported repository type"));
            }
            return avgSq.zipWith(avg, (a, b) -> (a != null && b != null) ? a - (b * b) : 0.0);
        });
    }

    // --- Lateral Force Aggregations (Right) ---
    @Override
    public Mono<Double> getAverageLateralForceRight(Integer trainNo) {
        return executeQuery(trainNo, repo -> {
            if (repo instanceof HaugfjellMP1AxlesRepository) {
                return ((HaugfjellMP1AxlesRepository) repo).findAverageLateralForceRightByTrainNo(trainNo);
            } else if (repo instanceof HaugfjellMP3AxlesRepository) {
                return ((HaugfjellMP3AxlesRepository) repo).findAverageLateralForceRightByTrainNo(trainNo);
            } else {
                return Mono.error(new IllegalStateException("Unsupported repository type"));
            }
        });
    }

    @Override
    public Mono<Double> getMinLateralForceRight(Integer trainNo) {
        return executeQuery(trainNo, repo -> {
            if (repo instanceof HaugfjellMP1AxlesRepository) {
                return ((HaugfjellMP1AxlesRepository) repo).findMinLateralForceRightByTrainNo(trainNo);
            } else if (repo instanceof HaugfjellMP3AxlesRepository) {
                return ((HaugfjellMP3AxlesRepository) repo).findMinLateralForceRightByTrainNo(trainNo);
            } else {
                return Mono.error(new IllegalStateException("Unsupported repository type"));
            }
        });
    }

    @Override
    public Mono<Double> getMaxLateralForceRight(Integer trainNo) {
        return executeQuery(trainNo, repo -> {
            if (repo instanceof HaugfjellMP1AxlesRepository) {
                return ((HaugfjellMP1AxlesRepository) repo).findMaxLateralForceRightByTrainNo(trainNo);
            } else if (repo instanceof HaugfjellMP3AxlesRepository) {
                return ((HaugfjellMP3AxlesRepository) repo).findMaxLateralForceRightByTrainNo(trainNo);
            } else {
                return Mono.error(new IllegalStateException("Unsupported repository type"));
            }
        });
    }

    @Override
    public Mono<Double> getLateralForceRightVariance(Integer trainNo) {
        return executeQuery(trainNo, repo -> {
            Mono<Double> avgSq;
            Mono<Double> avg;
            if (repo instanceof HaugfjellMP1AxlesRepository) {
                avgSq = ((HaugfjellMP1AxlesRepository) repo).findAverageSquareLateralForceRightByTrainNo(trainNo);
                avg = ((HaugfjellMP1AxlesRepository) repo).findAverageLateralForceRightByTrainNo(trainNo);
            } else if (repo instanceof HaugfjellMP3AxlesRepository) {
                avgSq = ((HaugfjellMP3AxlesRepository) repo).findAverageSquareLateralForceRightByTrainNo(trainNo);
                avg = ((HaugfjellMP3AxlesRepository) repo).findAverageLateralForceRightByTrainNo(trainNo);
            } else {
                return Mono.error(new IllegalStateException("Unsupported repository type"));
            }
            return avgSq.zipWith(avg, (a, b) -> (a != null && b != null) ? a - (b * b) : 0.0);
        });
    }

    // --- Lateral Vibration Aggregations (Left) ---
    @Override
    public Mono<Double> getAverageLateralVibrationLeft(Integer trainNo) {
        return executeQuery(trainNo, repo -> {
            if (repo instanceof HaugfjellMP1AxlesRepository) {
                return ((HaugfjellMP1AxlesRepository) repo).findAverageLateralVibrationLeftByTrainNo(trainNo);
            } else if (repo instanceof HaugfjellMP3AxlesRepository) {
                return ((HaugfjellMP3AxlesRepository) repo).findAverageLateralVibrationLeftByTrainNo(trainNo);
            } else {
                return Mono.error(new IllegalStateException("Unsupported repository type"));
            }
        });
    }

    @Override
    public Mono<Double> getMinLateralVibrationLeft(Integer trainNo) {
        return executeQuery(trainNo, repo -> {
            if (repo instanceof HaugfjellMP1AxlesRepository) {
                return ((HaugfjellMP1AxlesRepository) repo).findMinLateralVibrationLeftByTrainNo(trainNo);
            } else if (repo instanceof HaugfjellMP3AxlesRepository) {
                return ((HaugfjellMP3AxlesRepository) repo).findMinLateralVibrationLeftByTrainNo(trainNo);
            } else {
                return Mono.error(new IllegalStateException("Unsupported repository type"));
            }
        });
    }

    @Override
    public Mono<Double> getMaxLateralVibrationLeft(Integer trainNo) {
        return executeQuery(trainNo, repo -> {
            if (repo instanceof HaugfjellMP1AxlesRepository) {
                return ((HaugfjellMP1AxlesRepository) repo).findMaxLateralVibrationLeftByTrainNo(trainNo);
            } else if (repo instanceof HaugfjellMP3AxlesRepository) {
                return ((HaugfjellMP3AxlesRepository) repo).findMaxLateralVibrationLeftByTrainNo(trainNo);
            } else {
                return Mono.error(new IllegalStateException("Unsupported repository type"));
            }
        });
    }

    @Override
    public Mono<Double> getLateralVibrationLeftVariance(Integer trainNo) {
        return executeQuery(trainNo, repo -> {
            Mono<Double> avgSq;
            Mono<Double> avg;
            if (repo instanceof HaugfjellMP1AxlesRepository) {
                avgSq = ((HaugfjellMP1AxlesRepository) repo).findAverageSquareLateralVibrationLeftByTrainNo(trainNo);
                avg = ((HaugfjellMP1AxlesRepository) repo).findAverageLateralVibrationLeftByTrainNo(trainNo);
            } else if (repo instanceof HaugfjellMP3AxlesRepository) {
                avgSq = ((HaugfjellMP3AxlesRepository) repo).findAverageSquareLateralVibrationLeftByTrainNo(trainNo);
                avg = ((HaugfjellMP3AxlesRepository) repo).findAverageLateralVibrationLeftByTrainNo(trainNo);
            } else {
                return Mono.error(new IllegalStateException("Unsupported repository type"));
            }
            return avgSq.zipWith(avg, (a, b) -> (a != null && b != null) ? a - (b * b) : 0.0);
        });
    }

    // --- Lateral Vibration Aggregations (Right) ---
    @Override
    public Mono<Double> getAverageLateralVibrationRight(Integer trainNo) {
        return executeQuery(trainNo, repo -> {
            if (repo instanceof HaugfjellMP1AxlesRepository) {
                return ((HaugfjellMP1AxlesRepository) repo).findAverageLateralVibrationRightByTrainNo(trainNo);
            } else if (repo instanceof HaugfjellMP3AxlesRepository) {
                return ((HaugfjellMP3AxlesRepository) repo).findAverageLateralVibrationRightByTrainNo(trainNo);
            } else {
                return Mono.error(new IllegalStateException("Unsupported repository type"));
            }
        });
    }

    @Override
    public Mono<Double> getMinLateralVibrationRight(Integer trainNo) {
        return executeQuery(trainNo, repo -> {
            if (repo instanceof HaugfjellMP1AxlesRepository) {
                return ((HaugfjellMP1AxlesRepository) repo).findMinLateralVibrationRightByTrainNo(trainNo);
            } else if (repo instanceof HaugfjellMP3AxlesRepository) {
                return ((HaugfjellMP3AxlesRepository) repo).findMinLateralVibrationRightByTrainNo(trainNo);
            } else {
                return Mono.error(new IllegalStateException("Unsupported repository type"));
            }
        });
    }

    @Override
    public Mono<Double> getMaxLateralVibrationRight(Integer trainNo) {
        return executeQuery(trainNo, repo -> {
            if (repo instanceof HaugfjellMP1AxlesRepository) {
                return ((HaugfjellMP1AxlesRepository) repo).findMaxLateralVibrationRightByTrainNo(trainNo);
            } else if (repo instanceof HaugfjellMP3AxlesRepository) {
                return ((HaugfjellMP3AxlesRepository) repo).findMaxLateralVibrationRightByTrainNo(trainNo);
            } else {
                return Mono.error(new IllegalStateException("Unsupported repository type"));
            }
        });
    }

    @Override
    public Mono<Double> getLateralVibrationRightVariance(Integer trainNo) {
        return executeQuery(trainNo, repo -> {
            Mono<Double> avgSq;
            Mono<Double> avg;
            if (repo instanceof HaugfjellMP1AxlesRepository) {
                avgSq = ((HaugfjellMP1AxlesRepository) repo).findAverageSquareLateralVibrationRightByTrainNo(trainNo);
                avg = ((HaugfjellMP1AxlesRepository) repo).findAverageLateralVibrationRightByTrainNo(trainNo);
            } else if (repo instanceof HaugfjellMP3AxlesRepository) {
                avgSq = ((HaugfjellMP3AxlesRepository) repo).findAverageSquareLateralVibrationRightByTrainNo(trainNo);
                avg = ((HaugfjellMP3AxlesRepository) repo).findAverageLateralVibrationRightByTrainNo(trainNo);
            } else {
                return Mono.error(new IllegalStateException("Unsupported repository type"));
            }
            return avgSq.zipWith(avg, (a, b) -> (a != null && b != null) ? a - (b * b) : 0.0);
        });
    }
}
