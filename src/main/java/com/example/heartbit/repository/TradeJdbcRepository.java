package com.example.heartbit.repository;

import com.example.heartbit.domain.Trade;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class TradeJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public void bulkInsertWithKeys(List<Trade> trades) {
        String sql = "INSERT INTO trade (trade_price, trade_count, trade_close, taker_type, trade_buy_id, trade_sell_id, trade_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            try (PreparedStatement ps = connection.prepareStatement(sql, new String[]{"trade_id"})) {

                for (Trade trade : trades) {
                    ps.setBigDecimal(1, trade.getTradePrice());
                    ps.setBigDecimal(2, trade.getTradeCount());
                    ps.setBigDecimal(3, trade.getTradeClosePrice());
                    ps.setString(4, trade.getTakerType());
                    ps.setLong(5, trade.getBuyOrder().getOrderId());
                    ps.setLong(6, trade.getSellOrder().getOrderId());
                    ps.setTimestamp(7, Timestamp.valueOf(trade.getTradeTime()));
                    ps.addBatch(); // 배치에 추가
                }

                ps.executeBatch(); // 한 번에 DB로 전송

                // DB가 생성해준 PK(trade_id) 가져오기
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    int i = 0;
                    while (rs.next()) {
                        Long generatedId = rs.getLong(1);

                        trades.get(i).assignIdAfterBulkInsert(generatedId);
                        i++;
                    }
                }
            }
            return null;
        });
    }
}