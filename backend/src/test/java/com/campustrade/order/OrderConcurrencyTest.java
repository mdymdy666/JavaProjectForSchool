package com.campustrade.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import com.campustrade.common.BusinessException;

@SpringBootTest
@ActiveProfiles("test")
@Sql(scripts = "/schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class OrderConcurrencyTest {
    @Autowired
    private OrderService orderService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void allowsOnlyOneBuyerToCreateOrderForSameProduct() throws Exception {
        jdbcTemplate.update("""
                INSERT INTO users(username, password_hash, nickname, role, status) VALUES
                ('seller-c', 'x', '卖家', 'USER', 'NORMAL'),
                ('buyer-c1', 'x', '买家一', 'USER', 'NORMAL'),
                ('buyer-c2', 'x', '买家二', 'USER', 'NORMAL')
                """);
        jdbcTemplate.update("INSERT INTO categories(name, sort_order, enabled) VALUES ('并发测试', 1, 1)");
        jdbcTemplate.update("""
                INSERT INTO products(seller_id, category_id, title, description, price,
                    item_condition, status, view_count, version, deleted)
                VALUES (1, 1, '唯一商品', '并发抢购', 99.00, '九成新', 'APPROVED', 0, 0, 0)
                """);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Callable<Boolean> buyerOne = attempt(2L, ready, start);
            Callable<Boolean> buyerTwo = attempt(3L, ready, start);
            List<Future<Boolean>> futures = List.of(pool.submit(buyerOne), pool.submit(buyerTwo));
            ready.await();
            start.countDown();

            long successes = 0;
            for (Future<Boolean> future : futures) {
                if (future.get()) successes++;
            }
            assertThat(successes).isEqualTo(1);
            assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders", Integer.class)).isEqualTo(1);
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT status FROM products WHERE id = 1", String.class)).isEqualTo("SOLD");
        } finally {
            pool.shutdownNow();
        }
    }

    private Callable<Boolean> attempt(long buyerId, CountDownLatch ready, CountDownLatch start) {
        return () -> {
            ready.countDown();
            start.await();
            try {
                orderService.createOrder(buyerId, new OrderDtos.CreateOrderRequest(1L));
                return true;
            } catch (BusinessException expectedConflict) {
                return false;
            }
        };
    }
}
