package com.banenor.service;

import com.banenor.repository.HaugfjellMP1AxlesRepository;
import com.banenor.repository.HaugfjellMP3AxlesRepository;
import com.banenor.util.RepositoryResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class AggregationServiceImpl implements AggregationService {

    private final RepositoryResolver repositoryResolver;

    /**
     * Helper method to resolve the appropriate repository (MP1 or MP3) for the given train number
     * and execute the provided query function.
     */
    private <T> Mono<T> executeGlobalQuery(Integer trainNo,
                                           java.util.function.Function<Object, Mono<T>> queryFunction) {
        return repositoryResolver.resolveRepository(trainNo)
                .flatMap(queryFunction);
    }

    // --- SPEED AGGREGATIONS ---

    @Override
    public Mono<Double> getAverageSpeed(Integer trainNo) {
        return executeGlobalQuery(trainNo, repo -> {
            if (repo instanceof HaugfjellMP1AxlesRepository) {
                return ((HaugfjellMP1AxlesRepository) repo).findGlobalAverageSpeed();
            } else if (repo instanceof HaugfjellMP3AxlesRepository) {
                return ((HaugfjellMP3AxlesRepository) repo).findGlobalAverageSpeed();
            } else {
                return Mono.error(new IllegalStateException("Unsupported repository type"));
            }
        });
    }

    @Override
    public Mono<Double> getMinSpeed(Integer trainNo) {
        // No dedicated min query provided – fallback to the average value.
        return getAverageSpeed(trainNo);
    }

    @Override
    public Mono<Double> getMaxSpeed(Integer trainNo) {
        // No dedicated max query provided – fallback to the average value.
        return getAverageSpeed(trainNo);
    }

    @Override
    public Mono<Double> getSpeedVariance(Integer trainNo) {
        return executeGlobalQuery(trainNo, repo -> {
            Mono<Double> avgSpeed;
            Mono<Double> avgSquareSpeed;
            if (repo instanceof HaugfjellMP1AxlesRepository) {
                avgSpeed = ((HaugfjellMP1AxlesRepository) repo).findGlobalAverageSpeed();
                avgSquareSpeed = ((HaugfjellMP1AxlesRepository) repo).findGlobalAverageSquareSpeed();
            } else if (repo instanceof HaugfjellMP3AxlesRepository) {
                avgSpeed = ((HaugfjellMP3AxlesRepository) repo).findGlobalAverageSpeed();
                avgSquareSpeed = ((HaugfjellMP3AxlesRepository) repo).findGlobalAverageSquareSpeed();
            } else {
                return Mono.error(new IllegalStateException("Unsupported repository type"));
            }
            return avgSpeed.zipWith(avgSquareSpeed, (avg, avgSq) -> avgSq - (avg * avg));
        });
    }

    // --- ANGLE OF ATTACK (AOA) AGGREGATIONS ---

    @Override
    public Mono<Double> getAverageAoa(Integer trainNo) {
        return executeGlobalQuery(trainNo, repo -> {
            if (repo instanceof HaugfjellMP1AxlesRepository) {
                return ((HaugfjellMP1AxlesRepository) repo).findGlobalAverageAoa();
            } else if (repo instanceof HaugfjellMP3AxlesRepository) {
                return ((HaugfjellMP3AxlesRepository) repo).findGlobalAverageAoa();
            }
            return Mono.error(new IllegalStateException("Unsupported repository type"));
        });
    }

    @Override
    public Mono<Double> getMinAoa(Integer trainNo) {
        // Fallback for min AOA—no dedicated min query; returning average.
        return getAverageAoa(trainNo);
    }

    @Override
    public Mono<Double> getMaxAoa(Integer trainNo) {
        // Fallback for max AOA—returning average.
        return getAverageAoa(trainNo);
    }

    @Override
    public Mono<Double> getAoaVariance(Integer trainNo) {
        // No global query for square of aoa provided; return 0 variance as fallback.
        return Mono.just(0.0);
    }

    // --- VIBRATION AGGREGATIONS (Left) ---

    @Override
    public Mono<Double> getAverageVibrationLeft(Integer trainNo) {
        return executeGlobalQuery(trainNo, repo -> {
            if (repo instanceof HaugfjellMP1AxlesRepository) {
                return ((HaugfjellMP1AxlesRepository) repo).findGlobalAverageVibrationLeft();
            } else if (repo instanceof HaugfjellMP3AxlesRepository) {
                return ((HaugfjellMP3AxlesRepository) repo).findGlobalAverageVibrationLeft();
            }
            return Mono.error(new IllegalStateException("Unsupported repository type"));
        });
    }

    @Override
    public Mono<Double> getMinVibrationLeft(Integer trainNo) {
        return getAverageVibrationLeft(trainNo);
    }

    @Override
    public Mono<Double> getMaxVibrationLeft(Integer trainNo) {
        return getAverageVibrationLeft(trainNo);
    }

    @Override
    public Mono<Double> getVibrationLeftVariance(Integer trainNo) {
        // No average-square query provided for vibration left; fallback to 0.0.
        return Mono.just(0.0);
    }

    // --- VIBRATION AGGREGATIONS (Right) ---

    @Override
    public Mono<Double> getAverageVibrationRight(Integer trainNo) {
        return executeGlobalQuery(trainNo, repo -> {
            if (repo instanceof HaugfjellMP1AxlesRepository) {
                return ((HaugfjellMP1AxlesRepository) repo).findGlobalAverageVibrationRight();
            } else if (repo instanceof HaugfjellMP3AxlesRepository) {
                return ((HaugfjellMP3AxlesRepository) repo).findGlobalAverageVibrationRight();
            }
            return Mono.error(new IllegalStateException("Unsupported repository type"));
        });
    }

    @Override
    public Mono<Double> getMinVibrationRight(Integer trainNo) {
        return getAverageVibrationRight(trainNo);
    }

    @Override
    public Mono<Double> getMaxVibrationRight(Integer trainNo) {
        return getAverageVibrationRight(trainNo);
    }

    @Override
    public Mono<Double> getVibrationRightVariance(Integer trainNo) {
        return Mono.just(0.0);
    }

    // --- VERTICAL FORCE AGGREGATIONS (Left) ---

    @Override
    public Mono<Double> getAverageVerticalForceLeft(Integer trainNo) {
        return executeGlobalQuery(trainNo, repo -> {
            if (repo instanceof HaugfjellMP1AxlesRepository) {
                return ((HaugfjellMP1AxlesRepository) repo).findGlobalAverageVerticalForceLeft();
            } else if (repo instanceof HaugfjellMP3AxlesRepository) {
                return ((HaugfjellMP3AxlesRepository) repo).findGlobalAverageVerticalForceLeft();
            }
            return Mono.error(new IllegalStateException("Unsupported repository type"));
        });
    }

    @Override
    public Mono<Double> getMinVerticalForceLeft(Integer trainNo) {
        return getAverageVerticalForceLeft(trainNo);
    }

    @Override
    public Mono<Double> getMaxVerticalForceLeft(Integer trainNo) {
        return getAverageVerticalForceLeft(trainNo);
    }

    @Override
    public Mono<Double> getVerticalForceLeftVariance(Integer trainNo) {
        return executeGlobalQuery(trainNo, repo -> {
            Mono<Double> avgForce;
            Mono<Double> avgSquareForce;
            if (repo instanceof HaugfjellMP1AxlesRepository) {
                avgForce = ((HaugfjellMP1AxlesRepository) repo).findGlobalAverageVerticalForceLeft();
                // Using the dedicated query for vertical force left by train number (even though it still uses tp1)
                avgSquareForce = ((HaugfjellMP1AxlesRepository) repo).findAverageVerticalForceLeftByTrainNo(trainNo);
            } else if (repo instanceof HaugfjellMP3AxlesRepository) {
                avgForce = ((HaugfjellMP3AxlesRepository) repo).findGlobalAverageVerticalForceLeft();
                avgSquareForce = ((HaugfjellMP3AxlesRepository) repo).findAverageVerticalForceLeftByTrainNo(trainNo);
            } else {
                return Mono.error(new IllegalStateException("Unsupported repository type"));
            }
            return avgForce.zipWith(avgSquareForce, (avg, avgSq) -> avgSq - (avg * avg));
        });
    }

    // --- VERTICAL FORCE AGGREGATIONS (Right) ---

    @Override
    public Mono<Double> getAverageVerticalForceRight(Integer trainNo) {
        return executeGlobalQuery(trainNo, repo -> {
            if (repo instanceof HaugfjellMP1AxlesRepository) {
                return ((HaugfjellMP1AxlesRepository) repo).findGlobalAverageVerticalForceRight();
            } else if (repo instanceof HaugfjellMP3AxlesRepository) {
                return ((HaugfjellMP3AxlesRepository) repo).findGlobalAverageVerticalForceRight();
            }
            return Mono.error(new IllegalStateException("Unsupported repository type"));
        });
    }

    @Override
    public Mono<Double> getMinVerticalForceRight(Integer trainNo) {
        return getAverageVerticalForceRight(trainNo);
    }

    @Override
    public Mono<Double> getMaxVerticalForceRight(Integer trainNo) {
        return getAverageVerticalForceRight(trainNo);
    }

    @Override
    public Mono<Double> getVerticalForceRightVariance(Integer trainNo) {
        return executeGlobalQuery(trainNo, repo -> {
            Mono<Double> avgForce;
            Mono<Double> avgSquareForce;
            if (repo instanceof HaugfjellMP1AxlesRepository) {
                avgForce = ((HaugfjellMP1AxlesRepository) repo).findGlobalAverageVerticalForceRight();
                avgSquareForce = ((HaugfjellMP1AxlesRepository) repo).findAverageVerticalForceRightByTrainNo(trainNo);
            } else if (repo instanceof HaugfjellMP3AxlesRepository) {
                avgForce = ((HaugfjellMP3AxlesRepository) repo).findGlobalAverageVerticalForceRight();
                avgSquareForce = ((HaugfjellMP3AxlesRepository) repo).findAverageVerticalForceRightByTrainNo(trainNo);
            } else {
                return Mono.error(new IllegalStateException("Unsupported repository type"));
            }
            return avgForce.zipWith(avgSquareForce, (avg, avgSq) -> avgSq - (avg * avg));
        });
    }

    // --- LATERAL FORCE AGGREGATIONS (Left) ---

    @Override
    public Mono<Double> getAverageLateralForceLeft(Integer trainNo) {
        return executeGlobalQuery(trainNo, repo -> {
            if (repo instanceof HaugfjellMP1AxlesRepository) {
                return ((HaugfjellMP1AxlesRepository) repo).findGlobalAverageLateralForceLeft();
            } else if (repo instanceof HaugfjellMP3AxlesRepository) {
                return ((HaugfjellMP3AxlesRepository) repo).findGlobalAverageLateralForceLeft();
            }
            return Mono.error(new IllegalStateException("Unsupported repository type"));
        });
    }

    @Override
    public Mono<Double> getMinLateralForceLeft(Integer trainNo) {
        return getAverageLateralForceLeft(trainNo);
    }

    @Override
    public Mono<Double> getMaxLateralForceLeft(Integer trainNo) {
        return getAverageLateralForceLeft(trainNo);
    }

    @Override
    public Mono<Double> getLateralForceLeftVariance(Integer trainNo) {
        // No dedicated variance query—return 0.0 as fallback.
        return Mono.just(0.0);
    }

    // --- LATERAL FORCE AGGREGATIONS (Right) ---

    @Override
    public Mono<Double> getAverageLateralForceRight(Integer trainNo) {
        return executeGlobalQuery(trainNo, repo -> {
            if (repo instanceof HaugfjellMP1AxlesRepository) {
                return ((HaugfjellMP1AxlesRepository) repo).findGlobalAverageLateralForceRight();
            } else if (repo instanceof HaugfjellMP3AxlesRepository) {
                return ((HaugfjellMP3AxlesRepository) repo).findGlobalAverageLateralForceRight();
            }
            return Mono.error(new IllegalStateException("Unsupported repository type"));
        });
    }

    @Override
    public Mono<Double> getMinLateralForceRight(Integer trainNo) {
        return getAverageLateralForceRight(trainNo);
    }

    @Override
    public Mono<Double> getMaxLateralForceRight(Integer trainNo) {
        return getAverageLateralForceRight(trainNo);
    }

    @Override
    public Mono<Double> getLateralForceRightVariance(Integer trainNo) {
        return Mono.just(0.0);
    }

    // --- LATERAL VIBRATION AGGREGATIONS (Left) ---

    @Override
    public Mono<Double> getAverageLateralVibrationLeft(Integer trainNo) {
        return executeGlobalQuery(trainNo, repo -> {
            if (repo instanceof HaugfjellMP1AxlesRepository) {
                return ((HaugfjellMP1AxlesRepository) repo).findGlobalAverageLateralVibrationLeft();
            } else if (repo instanceof HaugfjellMP3AxlesRepository) {
                return ((HaugfjellMP3AxlesRepository) repo).findGlobalAverageLateralVibrationLeft();
            }
            return Mono.error(new IllegalStateException("Unsupported repository type"));
        });
    }

    @Override
    public Mono<Double> getMinLateralVibrationLeft(Integer trainNo) {
        return getAverageLateralVibrationLeft(trainNo);
    }

    @Override
    public Mono<Double> getMaxLateralVibrationLeft(Integer trainNo) {
        return getAverageLateralVibrationLeft(trainNo);
    }

    @Override
    public Mono<Double> getLateralVibrationLeftVariance(Integer trainNo) {
        return Mono.just(0.0);
    }

    // --- LATERAL VIBRATION AGGREGATIONS (Right) ---

    @Override
    public Mono<Double> getAverageLateralVibrationRight(Integer trainNo) {
        return executeGlobalQuery(trainNo, repo -> {
            if (repo instanceof HaugfjellMP1AxlesRepository) {
                return ((HaugfjellMP1AxlesRepository) repo).findGlobalAverageLateralVibrationRight();
            } else if (repo instanceof HaugfjellMP3AxlesRepository) {
                return ((HaugfjellMP3AxlesRepository) repo).findGlobalAverageLateralVibrationRight();
            }
            return Mono.error(new IllegalStateException("Unsupported repository type"));
        });
    }

    @Override
    public Mono<Double> getMinLateralVibrationRight(Integer trainNo) {
        return getAverageLateralVibrationRight(trainNo);
    }

    @Override
    public Mono<Double> getMaxLateralVibrationRight(Integer trainNo) {
        return getAverageLateralVibrationRight(trainNo);
    }

    @Override
    public Mono<Double> getLateralVibrationRightVariance(Integer trainNo) {
        return Mono.just(0.0);
    }
}