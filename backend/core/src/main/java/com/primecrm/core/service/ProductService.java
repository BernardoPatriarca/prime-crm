package com.primecrm.core.service;

import com.primecrm.core.audit.AuditService;
import com.primecrm.core.dto.product.ProductListFilter;
import com.primecrm.core.dto.product.ProductRequest;
import com.primecrm.core.dto.product.ProductResponse;
import com.primecrm.core.mapper.ProductMapper;
import com.primecrm.core.service.support.CommercialReferenceResolver;
import com.primecrm.core.specification.ProductSpecifications;
import com.primecrm.core.specification.SpecificationUtils;
import com.primecrm.infra.entity.product.Product;
import com.primecrm.infra.repository.ProductRepository;
import com.primecrm.shared.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final CommercialReferenceResolver referenceResolver;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Page<ProductResponse> list(ProductListFilter filter, Pageable pageable) {
        return productRepository.findAll(toSpecification(filter), pageable).map(productMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(UUID id) {
        return productMapper.toResponse(getActiveOrThrow(id));
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        Product product = productMapper.toEntity(request);
        applyReferences(product, request);

        product = productRepository.save(product);
        auditService.recordCreate(product);
        return productMapper.toResponse(product);
    }

    @Transactional
    public ProductResponse update(UUID id, ProductRequest request) {
        Product product = getActiveOrThrow(id);
        Map<String, Object> previousState = auditService.snapshot(product);

        productMapper.updateEntity(product, request);
        applyReferences(product, request);

        product = productRepository.save(product);
        auditService.recordUpdate(product, previousState);
        return productMapper.toResponse(product);
    }

    @Transactional
    public void delete(UUID id) {
        Product product = getActiveOrThrow(id);
        product.setDeletedAt(Instant.now());
        productRepository.save(product);
        auditService.recordDelete(product);
    }

    private Specification<Product> toSpecification(ProductListFilter filter) {
        return SpecificationUtils.and(
                ProductSpecifications.notDeleted(),
                ProductSpecifications.withReferencesFetched(),
                ProductSpecifications.textSearch(filter.search()),
                ProductSpecifications.hasCategory(filter.categoryId()),
                ProductSpecifications.hasUnit(filter.unitId()),
                ProductSpecifications.isService(filter.service()),
                ProductSpecifications.isActive(filter.active()));
    }

    private void applyReferences(Product product, ProductRequest request) {
        product.setCategory(referenceResolver.domainValue(request.categoryId(), "Categoria"));
        product.setUnit(referenceResolver.domainValue(request.unitId(), "Unidade de medida"));
    }

    private Product getActiveOrThrow(UUID id) {
        return productRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produto", id));
    }
}
