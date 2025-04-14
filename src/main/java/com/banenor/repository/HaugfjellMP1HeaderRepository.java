package com.banenor.repository;

import com.banenor.model.HaugfjellMP1Header;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import java.time.LocalDateTime;

@Repository
public interface HaugfjellMP1HeaderRepository extends R2dbcRepository<HaugfjellMP1Header, Integer> {

    // Retrieve the header by train number.
    Mono<HaugfjellMP1Header> findByTrainNo(Integer trainNo);

    @Query("SELECT mstation FROM haugfjell_mp1_header WHERE train_no = :trainNo")
    Mono<String> findMStationByTrainNo(Integer trainNo);

    @Query("SELECT mplace FROM haugfjell_mp1_header WHERE train_no = :trainNo")
    Mono<String> findMPlaceByTrainNo(Integer trainNo);

    @Query("SELECT coo_lat FROM haugfjell_mp1_header WHERE train_no = :trainNo")
    Mono<Double> findCooLatByTrainNo(Integer trainNo);

    @Query("SELECT coo_long FROM haugfjell_mp1_header WHERE train_no = :trainNo")
    Mono<Double> findCooLongByTrainNo(Integer trainNo);

    @Query("SELECT mstart_time FROM haugfjell_mp1_header WHERE train_no = :trainNo")
    Mono<LocalDateTime> findMstartTimeByTrainNo(Integer trainNo);

    @Query("SELECT mstop_time FROM haugfjell_mp1_header WHERE train_no = :trainNo")
    Mono<LocalDateTime> findMstopTimeByTrainNo(Integer trainNo);

    // Retrieve a header by its mstart_time (useful for deduplication during insert operations)
    Mono<HaugfjellMP1Header> findByMstartTime(LocalDateTime mstartTime);
}
