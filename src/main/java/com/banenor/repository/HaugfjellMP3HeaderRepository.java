package com.banenor.repository;

import com.banenor.model.HaugfjellMP3Header;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

public interface HaugfjellMP3HeaderRepository extends ReactiveCrudRepository<HaugfjellMP3Header, Integer> {

     @Query("""
        SELECT 
          h.id,
          h.train_no,
          h.created_at,
          h.mstart_time,
          h.coo_lat,
          h.coo_long
        FROM haugfjell_mp3_header AS h
        WHERE h.train_no = :trainNo
          AND h.created_at BETWEEN :start AND :end
        ORDER BY h.created_at DESC
        OFFSET :page * :size
        LIMIT :size
        """)
    Flux<HaugfjellMP3Header> findHeaderWithAxleAggregates(
            Integer trainNo,
            LocalDateTime start,
            LocalDateTime end,
            List<String> fields,
            int page,
            int size
    );

    Mono<HaugfjellMP3Header> findByMstartTime(LocalDateTime mstartTime);

    // Retrieve the header by train number.
    Mono<HaugfjellMP3Header> findByTrainNo(Integer trainNo);

    @Query("SELECT mstation FROM haugfjell_mp3_header WHERE train_no = :trainNo")
    Mono<String> findMStationByTrainNo(Integer trainNo);

    @Query("SELECT mplace FROM haugfjell_mp3_header WHERE train_no = :trainNo")
    Mono<String> findMPlaceByTrainNo(Integer trainNo);

    @Query("SELECT coo_lat FROM haugfjell_mp3_header WHERE train_no = :trainNo")
    Mono<Double> findCooLatByTrainNo(Integer trainNo);

    @Query("SELECT coo_long FROM haugfjell_mp3_header WHERE train_no = :trainNo")
    Mono<Double> findCooLongByTrainNo(Integer trainNo);

    @Query("SELECT mstart_time FROM haugfjell_mp3_header WHERE train_no = :trainNo")
    Mono<LocalDateTime> findMstartTimeByTrainNo(Integer trainNo);

    @Query("SELECT mstop_time FROM haugfjell_mp3_header WHERE train_no = :trainNo")
    Mono<LocalDateTime> findMstopTimeByTrainNo(Integer trainNo);

}
