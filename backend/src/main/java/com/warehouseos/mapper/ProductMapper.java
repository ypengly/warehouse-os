package com.warehouseos.mapper;

import com.warehouseos.dto.product.ProductResponse;
import com.warehouseos.entity.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public ProductResponse toResponse(Product p) {
        return new ProductResponse(
                p.getId(), p.getSku(), p.getBarcode(), p.getName(), p.getDescription(),
                p.getCategory() != null ? p.getCategory().getId() : null,
                p.getCategory() != null ? p.getCategory().getName() : null,
                p.getBrand(), p.getUnitOfMeasure(), p.getCostPrice(), p.getSellingPrice(),
                p.getMinStockLevel(), p.getMaxStockLevel(), p.getReorderLevel(),
                p.getWeightGrams(), p.getLengthMm(), p.getWidthMm(), p.getHeightMm(),
                p.getImageUrl(), p.isActive(), p.getCreatedAt(), p.getUpdatedAt());
    }
}
