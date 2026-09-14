package com.primecrm.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.primecrm.core.audit.AuditService;
import com.primecrm.core.dto.product.ProductRequest;
import com.primecrm.core.mapper.ProductMapper;
import com.primecrm.core.service.support.CommercialReferenceResolver;
import com.primecrm.infra.entity.product.Product;
import com.primecrm.infra.repository.ProductRepository;
import com.primecrm.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private CommercialReferenceResolver referenceResolver;
    @Mock
    private AuditService auditService;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository, productMapper, referenceResolver, auditService);
    }

    @Test
    void create_savesTheProductAndAudits() {
        ProductRequest request = request();
        Product product = newProduct();

        when(productMapper.toEntity(request)).thenReturn(product);
        when(productRepository.save(product)).thenReturn(product);

        productService.create(request);

        verify(auditService).recordCreate(product);
    }

    @Test
    void update_appliesChangesAndAudits() {
        ProductRequest request = request();
        Product product = newProduct();
        when(productRepository.findByIdAndDeletedAtIsNull(product.getId())).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);

        productService.update(product.getId(), request);

        verify(auditService).recordUpdate(any(Product.class), any());
    }

    @Test
    void delete_marksTheProductAsDeletedAndAudits() {
        Product product = newProduct();
        when(productRepository.findByIdAndDeletedAtIsNull(product.getId())).thenReturn(Optional.of(product));

        productService.delete(product.getId());

        assertThat(product.getDeletedAt()).isNotNull();
        verify(auditService).recordDelete(product);
    }

    @Test
    void findById_withUnknownId_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(productRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findById(id)).isInstanceOf(ResourceNotFoundException.class);
    }

    private ProductRequest request() {
        return new ProductRequest("Consultoria de implantacao", null, null, null, null,
                new BigDecimal("1500.00"), null, true, true);
    }

    private Product newProduct() {
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setName("Consultoria de implantacao");
        product.setUnitPrice(new BigDecimal("1500.00"));
        return product;
    }
}
