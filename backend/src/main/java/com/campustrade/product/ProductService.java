package com.campustrade.product;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class ProductService {

    public List<ProductSummary> featuredProducts() {
        return List.of(
                new ProductSummary(
                        1L,
                        "九成新机械键盘",
                        "数码配件",
                        new BigDecimal("129.00"),
                        "九成新",
                        "计科小李",
                        "/demo/products/keyboard.jpg"),
                new ProductSummary(
                        2L,
                        "考研数学复习全书",
                        "图书教材",
                        new BigDecimal("35.00"),
                        "轻微笔记",
                        "经管小周",
                        "/demo/products/book.jpg"),
                new ProductSummary(
                        3L,
                        "宿舍折叠桌",
                        "生活用品",
                        new BigDecimal("48.00"),
                        "八成新",
                        "软件小陈",
                        "/demo/products/table.jpg"));
    }
}
