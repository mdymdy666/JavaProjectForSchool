package com.campustrade.product;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campustrade.common.ApiResponse;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/featured")
    public ApiResponse<List<ProductSummary>> featuredProducts() {
        return ApiResponse.success(productService.featuredProducts());
    }
}
