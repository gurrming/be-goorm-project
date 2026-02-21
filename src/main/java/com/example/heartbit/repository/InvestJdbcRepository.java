package com.example.heartbit.repository;

import com.example.heartbit.domain.Invest;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class InvestJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public void bulkInsert(List<Invest> invests) {
        // trade_id는 이제 객체 참조가 아니라 Long 타입 컬럼입니다.
        String sql = "INSERT INTO invest (invest_count, invest_price, trade_id, category_id, member_id) " +
                "VALUES (?, ?, ?, ?, ?)";

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Invest invest = invests.get(i);
                ps.setBigDecimal(1, invest.getInvestCount());
                ps.setBigDecimal(2, invest.getInvestPrice());
                ps.setLong(3, invest.getTradeId()); // 객체 대신 ID 값 삽입
                ps.setLong(4, invest.getCategory().getCategoryId());
                ps.setLong(5, invest.getMember().getMemberId());
            }

            @Override
            public int getBatchSize() {
                return invests.size();
            }
        });
    }
}