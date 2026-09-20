package com.warehouseos.specification;

import com.warehouseos.entity.Product;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

/**
 * Criteria-API predicates. Everything becomes a bound parameter, so free-text
 * search cannot be used for SQL injection.
 */
public final class ProductSpecifications {

    private ProductSpecifications() {}

    /** Matches SKU, name, barcode or brand, case-insensitively. */
    public static Specification<Product> search(String term) {
        if (!StringUtils.hasText(term)) {
            return null;
        }
        String pattern = "%" + term.trim().toLowerCase() + "%";
        return (root, query, cb) -> {
            Predicate sku = cb.like(cb.lower(root.get("sku")), pattern);
            Predicate name = cb.like(cb.lower(root.get("name")), pattern);
            Predicate barcode = cb.like(cb.lower(root.get("barcode")), pattern);
            Predicate brand = cb.like(cb.lower(root.get("brand")), pattern);
            return cb.or(sku, name, barcode, brand);
        };
    }

    public static Specification<Product> hasCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Product> isActive(Boolean active) {
        if (active == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("active"), active);
    }

    public static Specification<Product> hasBrand(String brand) {
        if (!StringUtils.hasText(brand)) {
            return null;
        }
        return (root, query, cb) -> cb.equal(cb.lower(root.get("brand")), brand.toLowerCase());
    }
}
