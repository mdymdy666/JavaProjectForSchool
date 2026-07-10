package com.campustrade.product;

import static com.campustrade.product.ProductDtos.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campustrade.auth.AuthMapper;
import com.campustrade.auth.UserAccount;
import com.campustrade.common.BusinessException;
import com.campustrade.common.ErrorCode;
import com.campustrade.common.PageResult;
import com.campustrade.security.SecurityUser;
import com.campustrade.cache.CacheNames;
import com.campustrade.cache.RedisSupport;
import com.campustrade.extension.ContentReviewService;

@Service
public class ProductService {
    private static final long MAX_PAGE_SIZE = 50;

    private final ProductMapper productMapper;
    private final ProductImageMapper productImageMapper;
    private final FavoriteMapper favoriteMapper;
    private final AuthMapper authMapper;
    private final RedisSupport redisSupport;
    private final ContentReviewService contentReview;

    public ProductService(
            ProductMapper productMapper,
            ProductImageMapper productImageMapper,
            FavoriteMapper favoriteMapper,
            AuthMapper authMapper,
            RedisSupport redisSupport,
            ContentReviewService contentReview) {
        this.productMapper = productMapper;
        this.productImageMapper = productImageMapper;
        this.favoriteMapper = favoriteMapper;
        this.authMapper = authMapper;
        this.redisSupport = redisSupport;
        this.contentReview = contentReview;
    }

    public PageResult<ProductCard> search(
            String keyword,
            Long categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String sort,
            long requestedPage,
            long requestedSize) {
        long pageNumber = Math.max(1, requestedPage);
        long pageSize = Math.max(1, Math.min(MAX_PAGE_SIZE, requestedSize));
        LambdaQueryWrapper<Product> query = new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, ProductStatus.APPROVED.name())
                .eq(categoryId != null, Product::getCategoryId, categoryId)
                .ge(minPrice != null, Product::getPrice, minPrice)
                .le(maxPrice != null, Product::getPrice, maxPrice);
        if (StringUtils.hasText(keyword)) {
            String normalized = keyword.trim();
            List<String> terms = searchTerms(normalized);
            List<Long> categoryIds = terms.stream()
                    .flatMap(term -> productMapper.categoryIdsLike(term).stream())
                    .distinct()
                    .toList();
            query.and(item -> {
                boolean first = true;
                for (String term : terms) {
                    if (first) {
                        item.like(Product::getTitle, term);
                        first = false;
                    } else {
                        item.or().like(Product::getTitle, term);
                    }
                    item.or().like(Product::getDescription, term)
                            .or().like(Product::getItemCondition, term);
                }
                if (!categoryIds.isEmpty()) {
                    item.or().in(Product::getCategoryId, categoryIds);
                }
            });
        }
        applySort(query, sort);
        Page<Product> result = productMapper.selectPage(new Page<>(pageNumber, pageSize), query);
        List<ProductCard> records = result.getRecords().stream().map(this::toCard).toList();
        return new PageResult<>(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    public List<ProductSummary> featuredProducts() {
        return search(null, null, null, null, "hot", 1, 6).records().stream()
                .map(card -> new ProductSummary(
                        card.id(), card.title(), card.categoryName(), card.price(),
                        card.itemCondition(), card.sellerNickname(), card.coverUrl()))
                .toList();
    }

    @Transactional
    public ProductDetail publish(long sellerId, PublishRequest request) {
        contentReview.assertClean(request.title());
        contentReview.assertClean(request.description());
        if (!productMapper.categoryExists(request.categoryId())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "商品分类不存在或已停用");
        }
        Product product = new Product();
        product.setSellerId(sellerId);
        product.setCategoryId(request.categoryId());
        product.setTitle(request.title().trim());
        product.setDescription(request.description().trim());
        product.setPrice(request.price());
        product.setItemCondition(request.itemCondition().trim());
        product.setStatus(ProductStatus.PENDING.name());
        product.setViewCount(0);
        product.setVersion(0);
        product.setDeleted(0);
        productMapper.insert(product);
        int sortOrder = 0;
        for (String imageUrl : request.imageUrls()) {
            ProductImage image = new ProductImage();
            image.setProductId(product.getId());
            image.setImageUrl(imageUrl);
            image.setSortOrder(sortOrder++);
            productImageMapper.insert(image);
        }
        return detailForOwner(product, sellerId);
    }

    @Transactional
    public ProductDetail edit(long sellerId, long productId, PublishRequest request) {
        contentReview.assertClean(request.title());
        contentReview.assertClean(request.description());
        Product product = requireOwnedProduct(sellerId, productId);
        if (ProductStatus.SOLD.name().equals(product.getStatus())) {
            throw new BusinessException(ErrorCode.INVALID_STATE, "已售出商品不可编辑");
        }
        if (!productMapper.categoryExists(request.categoryId())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "商品分类不存在或已停用");
        }
        product.setCategoryId(request.categoryId());
        product.setTitle(request.title().trim());
        product.setDescription(request.description().trim());
        product.setPrice(request.price());
        product.setItemCondition(request.itemCondition().trim());
        product.setStatus(ProductStatus.PENDING.name());
        productMapper.updateById(product);
        evict(productId);
        productImageMapper.delete(new LambdaQueryWrapper<ProductImage>()
                .eq(ProductImage::getProductId, productId));
        int sortOrder = 0;
        for (String imageUrl : request.imageUrls()) {
            ProductImage image = new ProductImage();
            image.setProductId(productId);
            image.setImageUrl(imageUrl);
            image.setSortOrder(sortOrder++);
            productImageMapper.insert(image);
        }
        return detailForOwner(product, sellerId);
    }

    public ProductDetail detail(long productId, SecurityUser viewer) {
        Product product = requireProduct(productId);
        boolean privileged = viewer != null
                && (product.getSellerId().equals(viewer.userId()) || "ADMIN".equals(viewer.role()));
        if (!ProductStatus.APPROVED.name().equals(product.getStatus()) && !privileged) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        productMapper.incrementViewCount(productId);
        product.setViewCount((product.getViewCount() == null ? 0 : product.getViewCount()) + 1);
        redisSupport.incrementScore(CacheNames.PRODUCT_HOT, String.valueOf(productId));
        Long viewerId = viewer == null ? null : viewer.userId();
        return toDetail(product, viewerId);
    }

    @Transactional
    public FavoriteResponse favorite(long userId, long productId) {
        Product product = requireProduct(productId);
        if (!ProductStatus.APPROVED.name().equals(product.getStatus())) {
            throw new BusinessException(ErrorCode.INVALID_STATE, "当前商品不可收藏");
        }
        long existing = favoriteMapper.selectCount(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getProductId, productId));
        if (existing > 0) {
            favoriteMapper.delete(new LambdaQueryWrapper<Favorite>()
                    .eq(Favorite::getUserId, userId)
                    .eq(Favorite::getProductId, productId));
            return new FavoriteResponse(productId, false);
        }
        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setProductId(productId);
        try {
            favoriteMapper.insert(favorite);
        } catch (DuplicateKeyException ignored) {
            // The unique key makes concurrent repeated favorites idempotent.
        }
        return new FavoriteResponse(productId, true);
    }

    @Transactional
    public ProductDetail offShelf(long sellerId, long productId) {
        Product product = requireOwnedProduct(sellerId, productId);
        if (!ProductStatus.APPROVED.name().equals(product.getStatus())) {
            throw new BusinessException(ErrorCode.INVALID_STATE);
        }
        product.setStatus(ProductStatus.OFF_SHELF.name());
        productMapper.updateById(product);
        evict(productId);
        return detailForOwner(product, sellerId);
    }

    @Transactional
    public ProductDetail onShelf(long sellerId, long productId) {
        Product product = requireOwnedProduct(sellerId, productId);
        if (!ProductStatus.OFF_SHELF.name().equals(product.getStatus())) {
            throw new BusinessException(ErrorCode.INVALID_STATE);
        }
        product.setStatus(ProductStatus.APPROVED.name());
        productMapper.updateById(product);
        evict(productId);
        return detailForOwner(product, sellerId);
    }

    @Transactional
    public ProductDetail softDelete(long sellerId, long productId) {
        Product product = requireOwnedProduct(sellerId, productId);
        if (ProductStatus.SOLD.name().equals(product.getStatus())) {
            throw new BusinessException(ErrorCode.INVALID_STATE, "已售出商品不可删除");
        }
        product.setStatus(ProductStatus.DELETED.name());
        ProductDetail response = detailForOwner(product, sellerId);
        if (productMapper.softDelete(productId, sellerId) != 1) {
            throw new BusinessException(ErrorCode.INVALID_STATE);
        }
        evict(productId);
        return response;
    }

    @Transactional
    public ProductDetail audit(long adminId, long productId, AuditRequest request) {
        Product product = requireProduct(productId);
        if (!ProductStatus.PENDING.name().equals(product.getStatus())) {
            throw new BusinessException(ErrorCode.INVALID_STATE, "商品已完成审核");
        }
        String status = request.approved()
                ? ProductStatus.APPROVED.name()
                : ProductStatus.REJECTED.name();
        product.setStatus(status);
        productMapper.updateById(product);
        evict(productId);
        productMapper.insertAuditLog(adminId, productId,
                request.approved() ? "APPROVE" : "REJECT", request.reason());
        productMapper.insertNotification(
                product.getSellerId(),
                "PRODUCT_AUDIT",
                "商品审核结果",
                "商品《" + product.getTitle() + "》审核结果：" + status);
        return toDetail(product, adminId);
    }

    public PageResult<ProductCard> pending(long page, long size) {
        long pageNumber = Math.max(1, page);
        long pageSize = Math.max(1, Math.min(MAX_PAGE_SIZE, size));
        Page<Product> result = productMapper.selectPage(
                new Page<>(pageNumber, pageSize),
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getStatus, ProductStatus.PENDING.name())
                        .orderByAsc(Product::getCreatedAt));
        return new PageResult<>(
                result.getRecords().stream().map(this::toCard).toList(),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    private ProductDetail detailForOwner(Product product, long ownerId) {
        return toDetail(product, ownerId);
    }

    private void evict(long productId) {
        redisSupport.delete(CacheNames.PRODUCT_DETAIL + productId);
    }

    private List<String> searchTerms(String keyword) {
        Set<String> terms = new LinkedHashSet<>();
        String normalized = keyword.trim();
        if (!StringUtils.hasText(normalized)) return List.of();
        terms.add(normalized);
        for (String part : normalized.split("[\\s,，、]+")) {
            if (StringUtils.hasText(part)) terms.add(part.trim());
        }
        for (int i = 0; i < normalized.length() - 1; i++) {
            String term = normalized.substring(i, i + 2).trim();
            if (StringUtils.hasText(term)) terms.add(term);
        }
        addSearchAliases(normalized, terms);
        return new ArrayList<>(terms);
    }

    private void addSearchAliases(String keyword, Set<String> terms) {
        if (keyword.contains("耳机")) {
            terms.add("AirPods");
            terms.add("耳塞");
            terms.add("降噪");
        }
        if (keyword.contains("资料")) {
            terms.add("复习");
            terms.add("真题");
        }
        if (keyword.contains("考研")) {
            terms.add("数学");
            terms.add("复习");
        }
    }

    private ProductDetail toDetail(Product product, Long viewerId) {
        UserAccount seller = authMapper.selectById(product.getSellerId());
        String sellerNickname = seller == null ? "未知用户" : seller.getNickname();
        String sellerAvatar = seller == null ? null : seller.getAvatarUrl();
        Long sellerProductCount = productMapper.selectCount(
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getSellerId, product.getSellerId())
                        .eq(Product::getStatus, ProductStatus.APPROVED.name())
                        .eq(Product::getDeleted, 0));
        List<String> images = productImageMapper.selectList(
                        new LambdaQueryWrapper<ProductImage>()
                                .eq(ProductImage::getProductId, product.getId())
                                .orderByAsc(ProductImage::getSortOrder))
                .stream().map(ProductImage::getImageUrl).toList();
        boolean favorite = viewerId != null && favoriteMapper.selectCount(
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, viewerId)
                        .eq(Favorite::getProductId, product.getId())) > 0;
        return new ProductDetail(
                product.getId(), product.getSellerId(), sellerNickname, sellerAvatar,
                sellerProductCount == null ? 0 : sellerProductCount.intValue(),
                product.getCategoryId(), productMapper.categoryName(product.getCategoryId()),
                product.getTitle(), product.getDescription(), product.getPrice(),
                product.getItemCondition(), product.getStatus(), product.getViewCount(),
                images, favorite, product.getCreatedAt());
    }

    private ProductCard toCard(Product product) {
        UserAccount seller = authMapper.selectById(product.getSellerId());
        ProductImage cover = productImageMapper.selectOne(
                new LambdaQueryWrapper<ProductImage>()
                        .eq(ProductImage::getProductId, product.getId())
                        .orderByAsc(ProductImage::getSortOrder)
                        .last("LIMIT 1"));
        return new ProductCard(
                product.getId(), product.getTitle(), product.getPrice(), product.getItemCondition(),
                product.getStatus(), product.getViewCount(), productMapper.categoryName(product.getCategoryId()),
                seller == null ? "未知用户" : seller.getNickname(),
                cover == null ? null : cover.getImageUrl(), product.getCreatedAt());
    }

    private Product requireProduct(long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        return product;
    }

    private Product requireOwnedProduct(long sellerId, long productId) {
        Product product = requireProduct(productId);
        if (!product.getSellerId().equals(sellerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return product;
    }

    private void applySort(LambdaQueryWrapper<Product> query, String sort) {
        switch (sort == null ? "newest" : sort) {
            case "priceAsc" -> query.orderByAsc(Product::getPrice).orderByDesc(Product::getId);
            case "priceDesc" -> query.orderByDesc(Product::getPrice).orderByDesc(Product::getId);
            case "hot" -> query.orderByDesc(Product::getViewCount).orderByDesc(Product::getId);
            default -> query.orderByDesc(Product::getCreatedAt).orderByDesc(Product::getId);
        }
    }
}
