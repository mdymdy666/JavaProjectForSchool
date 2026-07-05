package com.campustrade.admin;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.campustrade.common.ApiResponse;
import com.campustrade.common.PageResult;
import com.campustrade.product.ProductDtos.AuditRequest;
import com.campustrade.product.ProductDtos.ProductCard;
import com.campustrade.product.ProductDtos.ProductDetail;
import com.campustrade.product.ProductService;
import com.campustrade.security.SecurityUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/products")
public class AdminProductController {
    private final ProductService productService;

    public AdminProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/pending")
    public ApiResponse<PageResult<ProductCard>> pending(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return ApiResponse.success(productService.pending(page, size));
    }

    @PostMapping("/{id}/audit")
    public ApiResponse<ProductDetail> audit(
            @PathVariable long id,
            @Valid @RequestBody AuditRequest request,
            Authentication authentication) {
        SecurityUser admin = (SecurityUser) authentication.getPrincipal();
        return ApiResponse.success(productService.audit(admin.userId(), id, request));
    }
}
