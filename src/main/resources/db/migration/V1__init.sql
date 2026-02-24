BEGIN;

-- =====================
-- MEMBER
-- =====================
CREATE TABLE IF NOT EXISTS member (
                                      member_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                      member_email VARCHAR(30) UNIQUE,
    member_password VARCHAR(100),
    member_nickname VARCHAR(20)
    );

CREATE TABLE IF NOT EXISTS bots (
                                    bot_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY
);

-- =====================
-- CATEGORY
-- =====================
CREATE TABLE IF NOT EXISTS category (
                                        category_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                        symbol VARCHAR(10) NOT NULL UNIQUE,
    category_name VARCHAR(50)
    active BOOLEAN DEFAULT TRUE;
    );

-- =====================
-- INTEREST
-- =====================
CREATE TABLE IF NOT EXISTS interest (
                                        interest_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                        member_id BIGINT NOT NULL REFERENCES member(member_id),
    category_id BIGINT NOT NULL REFERENCES category(category_id),
    CONSTRAINT uq_interest_member_category UNIQUE (member_id, category_id)
    );

-- =====================
-- ORDERS
-- =====================
CREATE TABLE IF NOT EXISTS orders (
                                      order_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                      order_price NUMERIC(18,8),
    order_count NUMERIC(18,8),
    remaining_count NUMERIC(18,8) NOT NULL CHECK (remaining_count >= 0),
    order_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    order_type VARCHAR(10) NOT NULL CHECK (order_type IN ('BUY','SELL')),
    order_status VARCHAR(10) NOT NULL CHECK (order_status IN ('OPEN','PARTIAL','FILLED','CANCELLED')),
    member_id BIGINT REFERENCES member(member_id),
    bot_id BIGINT REFERENCES bots(bot_id),
    category_id BIGINT NOT NULL REFERENCES category(category_id)
    );

-- =====================
-- TRADE
-- =====================
CREATE TABLE IF NOT EXISTS trade (
                                     trade_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                     trade_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                     trade_price NUMERIC(18,8) NOT NULL,
    trade_count NUMERIC(18,8) NOT NULL,
    trade_buy_id BIGINT NOT NULL REFERENCES orders(order_id),
    trade_sell_id BIGINT NOT NULL REFERENCES orders(order_id),
    trade_close NUMERIC(18,8),
    taker_type VARCHAR(10)
    );

-- =====================
-- INVEST
-- =====================
CREATE TABLE IF NOT EXISTS invest (
                                      invest_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                      invest_count NUMERIC(18,8),
    invest_price NUMERIC(18,8),
    trade_id BIGINT NOT NULL REFERENCES trade(trade_id),
    category_id BIGINT NOT NULL REFERENCES category(category_id),
    member_id BIGINT NOT NULL REFERENCES member(member_id)
    );

-- =====================
-- ASSET
-- =====================
CREATE TABLE IF NOT EXISTS asset (
                                     asset_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                     member_id BIGINT NOT NULL UNIQUE REFERENCES member(member_id),
    asset_cash NUMERIC(18,8) DEFAULT 0,
    asset_canorder NUMERIC(18,8) DEFAULT 0

    );

-- =====================
-- CHATROOM
-- =====================
CREATE TABLE IF NOT EXISTS chatroom (
                                        chatroom_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                        category_id BIGINT NOT NULL REFERENCES category(category_id),
    member_id BIGINT NOT NULL REFERENCES member(member_id),
    chatroom_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    chatroom_content VARCHAR(500)
    );

-- =====================
-- NOTIFICATION
-- =====================
CREATE TABLE IF NOT EXISTS notification (
                                            notification_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                            member_id BIGINT NOT NULL REFERENCES member(member_id),
    notification_content VARCHAR(200) NOT NULL,
    notification_type VARCHAR(50) NOT NULL CHECK (notification_type IN ('TRADE', 'SYSTEM')),
    notification_is_read BOOLEAN NOT null DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

CREATE TABLE news_data (
                           news_id SERIAL PRIMARY KEY,
                           category_id BIGINT,
                           title TEXT NOT NULL,
                           description TEXT,
                           published_at TIMESTAMP WITH TIME ZONE,
                           symbol VARCHAR(20),
                           hash_key TEXT NOT NULL UNIQUE,
                           is_test BOOLEAN DEFAULT FALSE,
                           sentiment_score REAL DEFAULT 0.5,
                           sentiment_label VARCHAR(20)
);


CREATE TABLE community_data (
                                community_id BIGSERIAL PRIMARY KEY,
                                category_id BIGINT,
                                title TEXT NOT NULL,
                                description TEXT,
                                published_at TIMESTAMP WITH TIME ZONE,
                                symbol VARCHAR(20),
                                platform VARCHAR(50) DEFAULT 'reddit',
                                hash_key TEXT NOT NULL UNIQUE,
                                ups INTEGER DEFAULT 0,
                                is_test BOOLEAN DEFAULT FALSE,
                                sentiment_score REAL DEFAULT 0.5,
                                sentiment_label VARCHAR(20)
);

CREATE TABLE sentiment_result (
                                  result_id BIGSERIAL PRIMARY KEY,
                                  category_id BIGINT NOT NULL UNIQUE,
                                  total_score DOUBLE PRECISION DEFAULT 0.5,
                                  total_label VARCHAR(10),
                                  news_score DOUBLE PRECISION DEFAULT 0.5,
                                  community_score DOUBLE PRECISION DEFAULT 0.5,
                                  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                  CONSTRAINT fk_sentiment_category FOREIGN KEY (category_id)
                                      REFERENCES category(category_id) ON DELETE CASCADE
);

-- =====================
-- CATEGORY DATA
-- =====================
INSERT INTO category (symbol, category_name) VALUES
                                                 ('BTC', '비트코인'),
                                                 ('ETH', '이더리움'),
                                                 ('XRP', '리플'),
                                                 ('SOL', '솔라나'),
                                                 ('ADA', '에이다'),
                                                 ('DOGE', '도지코인'),
                                                 ('TRX', '트론'),
                                                 ('DOT', '폴카닷'),
                                                 ('LINK', '체인링크'),
                                                 ('MATIC', '폴리곤'),
                                                 ('SHIB', '시바이누'),
                                                 ('LTC', '라이트코인'),
                                                 ('ATOM', '코스모스'),
                                                 ('EOS', '이오스'),
                                                 ('FIL', '파일코인'),
                                                 ('ALGO', '알고랜드')
    ON CONFLICT (symbol) DO UPDATE
                                SET category_name = EXCLUDED.category_name;


COMMIT;