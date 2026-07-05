package com.campustrade.product;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.campustrade.common.ApiResponse;
import com.campustrade.common.PageResult;
import com.campustrade.product.ProductDtos.FavoriteResponse;
import com.campustrade.product.ProductDtos.ProductCard;
import com.campustrade.product.ProductDtos.ProductDetail;
import com.campustrade.product.ProductDtos.PublishRequest;
import com.campustrade.security.SecurityUser;

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ApiResponse<PageResult<ProductCard>> products(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "12") long size) {
        return ApiResponse.success(productService.search(
                keyword, categoryId, minPrice, maxPrice, sort, page, size));
    }

    @GetMapping("/featured")
    public ApiResponse<List<ProductSummary>> featuredProducts() {
        return ApiResponse.success(productService.featuredProducts());
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductDetail> detail(@PathVariable long id, Authentication authentication) {
        return ApiResponse.success(productService.detail(id, currentUser(authentication)));
    }

    @PostMapping
    public ApiResponse<ProductDetail> publish(
            @Valid @RequestBody PublishRequest request,
            Authentication authentication) {
        return ApiResponse.success(productService.publish(requireUser(authentication).userId(), request));
    }

    @PutMapping("/{id}")
    public ApiResponse<ProductDetail> edit(
            @PathVariable long id,
            @Valid @RequestBody PublishRequest request,
            Authentication authentication) {
        return ApiResponse.success(productService.edit(requireUser(authentication).userId(), id, request));
    }

    @PostMapping("/{id}/favorite")
    public ApiResponse<FavoriteResponse> favorite(@PathVariable long id, Authentication authentication) {
        return ApiResponse.success(productService.favorite(requireUser(authentication).userId(), id));
    }

    @PostMapping("/{id}/off-shelf")
    public ApiResponse<ProductDetail> offShelf(@PathVariable long id, Authentication authentication) {
        return ApiResponse.success(productService.offShelf(requireUser(authentication).userId(), id));
    }

    @PostMapping("/{id}/on-shelf")
    public ApiResponse<ProductDetail> onShelf(@PathVariable long id, Authentication authentication) {
        return ApiResponse.success(productService.onShelf(requireUser(authentication).userId(), id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<ProductDetail> delete(@PathVariable long id, Authentication authentication) {
        return ApiResponse.success(productService.softDelete(requireUser(authentication).userId(), id));
    }

    private SecurityUser currentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof SecurityUser user)) {
            return null;
        }
        return user;
    }

    private SecurityUser requireUser(Authentication authentication) {
        SecurityUser user = currentUser(authentication);
        if (user == null) {
            throw new IllegalStateException("Authenticated endpoint has no user context");
        }
        return user;
    }
}
