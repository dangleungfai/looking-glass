package com.isp.lg.repository;

import com.isp.lg.domain.QueryLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface QueryLogRepository extends JpaRepository<QueryLog, Long> {
    Page<QueryLog> findByOrderByCreatedAtDesc(Pageable pageable);

    interface TrendRow {
        String getBucket();
        Long getTotal();
        Long getSuccess();
        Long getFailed();
    }

    interface TargetCountRow {
        String getTarget();
        Long getCnt();
    }

    interface SourceCountRow {
        String getSourceIp();
        Long getCnt();
    }

    @Query(value = """
            SELECT DATE_FORMAT(created_at, '%Y-%m-%d %H:00') AS bucket,
                   COUNT(*) AS total,
                   SUM(CASE WHEN UPPER(IFNULL(result_status, '')) = 'SUCCESS' THEN 1 ELSE 0 END) AS success,
                   SUM(CASE WHEN UPPER(IFNULL(result_status, '')) = 'SUCCESS' THEN 0 ELSE 1 END) AS failed
            FROM query_logs
            WHERE created_at >= :start
            GROUP BY DATE_FORMAT(created_at, '%Y-%m-%d %H:00')
            ORDER BY DATE_FORMAT(created_at, '%Y-%m-%d %H:00')
            """, nativeQuery = true)
    List<TrendRow> findHourlyTrendSince(@Param("start") Instant start);

    @Query(value = """
            SELECT query_target AS target, COUNT(*) AS cnt
            FROM query_logs
            WHERE created_at >= :start
            GROUP BY query_target
            ORDER BY cnt DESC
            LIMIT 10
            """, nativeQuery = true)
    List<TargetCountRow> findTopTargetsSince(@Param("start") Instant start);

    @Query("SELECT COUNT(q) FROM QueryLog q WHERE q.createdAt >= :start")
    long countTotalSince(@Param("start") Instant start);

    @Query("SELECT COUNT(q) FROM QueryLog q WHERE q.createdAt >= :start AND UPPER(COALESCE(q.resultStatus, '')) <> 'SUCCESS'")
    long countFailedSince(@Param("start") Instant start);

    @Query("SELECT COUNT(DISTINCT q.sourceIp) FROM QueryLog q WHERE q.createdAt >= :start")
    long countDistinctSourceIpSince(@Param("start") Instant start);

    @Query(value = """
            SELECT source_ip AS sourceIp, COUNT(*) AS cnt
            FROM query_logs
            WHERE created_at >= :start
              AND UPPER(IFNULL(result_status, '')) <> 'SUCCESS'
            GROUP BY source_ip
            ORDER BY cnt DESC
            LIMIT 5
            """, nativeQuery = true)
    List<SourceCountRow> findTopFailedSourceIpsSince(@Param("start") Instant start);
}
