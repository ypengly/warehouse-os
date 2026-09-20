package com.warehouseos.service;

import com.warehouseos.dto.common.PageResponse;
import com.warehouseos.dto.product.ProductRequest;
import com.warehouseos.dto.product.ProductResponse;
import com.warehouseos.entity.Category;
import com.warehouseos.entity.Product;
import com.warehouseos.exception.BusinessRuleException;
import com.warehouseos.exception.DuplicateResourceException;
import com.warehouseos.exception.ResourceNotFoundException;
import com.warehouseos.mapper.ProductMapper;
import com.warehouseos.repository.CategoryRepository;
import com.warehouseos.repository.ProductRepository;
import com.warehouseos.specification.ProductSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper mapper;

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> search(String term, Long categoryId, String brand,
                                                Boolean active, Pageable pageable) {
        Specification<Product> spec = Specification.allOf(
                ProductSpecifications.search(term),
                ProductSpecifications.hasCategory(categoryId),
                ProductSpecifications.hasBrand(brand),
                ProductSpecifications.isActive(active));
        return PageResponse.from(productRepository.findAll(spec, pageable), mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        return mapper.toResponse(findProduct(id));
    }

    @Transactional(readOnly = true)
    public ProductResponse getBySku(String sku) {
        return productRepository.findBySkuIgnoreCase(sku)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Product", sku));
    }

    /** Used by the barcode scanner flow. */
    @Transactional(readOnly = true)
    public ProductResponse getByBarcode(String barcode) {
        return productRepository.findByBarcode(barcode)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Product with barcode", barcode));
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Transactional
    public ProductResponse create(ProductRequest request) {
        if (productRepository.existsBySkuIgnoreCase(request.sku())) {
            throw new DuplicateResourceException("Product", "SKU", request.sku());
        }
        if (StringUtils.hasText(request.barcode())
                && productRepository.existsByBarcode(request.barcode())) {
            throw new DuplicateResourceException("Product", "barcode", request.barcode());
        }

        Product product = new Product();
        apply(request, product);
        product.setActive(request.active() == null || request.active());
        return mapper.toResponse(productRepository.save(product));
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = findProduct(id);

        if (!product.getSku().equalsIgnoreCase(request.sku())
                && productRepository.existsBySkuIgnoreCase(request.sku())) {
            throw new DuplicateResourceException("Product", "SKU", request.sku());
        }
        if (StringUtils.hasText(request.barcode())
                && !request.barcode().equals(product.getBarcode())
                && productRepository.existsByBarcode(request.barcode())) {
            throw new DuplicateResourceException("Product", "barcode", request.barcode());
        }

        apply(request, product);
        if (request.active() != null) {
            product.setActive(request.active());
        }
        return mapper.toResponse(product);
    }

    /**
     * Products are deactivated, never hard-deleted: stock movements and audit
     * rows reference them forever.
     */
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Transactional
    public void deactivate(Long id) {
        findProduct(id).setActive(false);
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Transactional
    public void activate(Long id) {
        findProduct(id).setActive(true);
    }

    private void apply(ProductRequest request, Product product) {
        if (request.maxStockLevel() != null
                && request.maxStockLevel() < request.minStockLevel()) {
            throw new BusinessRuleException(
                    "Maximum stock level cannot be lower than minimum stock level");
        }

        product.setSku(request.sku().toUpperCase());
        product.setBarcode(StringUtils.hasText(request.barcode()) ? request.barcode() : null);
        product.setName(request.name());
        product.setDescription(request.description());
        product.setBrand(request.brand());
        product.setUnitOfMeasure(request.unitOfMeasure());
        product.setCostPrice(request.costPrice());
        product.setSellingPrice(request.sellingPrice());
        product.setMinStockLevel(request.minStockLevel());
        product.setMaxStockLevel(request.maxStockLevel());
        product.setReorderLevel(request.reorderLevel());
        product.setWeightGrams(request.weightGrams());
        product.setLengthMm(request.lengthMm());
        product.setWidthMm(request.widthMm());
        product.setHeightMm(request.heightMm());
        product.setImageUrl(request.imageUrl());
        product.setCategory(resolveCategory(request.categoryId()));
    }

    private Category resolveCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", categoryId));
    }

    private Product findProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }
}
