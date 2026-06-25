package com.yas.order.service;

import static com.yas.order.utils.SecurityContextUtils.setUpSecurityContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.order.config.ServiceUrlConfig;
import com.yas.order.model.enumeration.DeliveryMethod;
import com.yas.order.model.enumeration.DeliveryStatus;
import com.yas.order.model.enumeration.OrderStatus;
import com.yas.order.model.enumeration.PaymentStatus;
import com.yas.order.viewmodel.order.OrderItemVm;
import com.yas.order.viewmodel.order.OrderVm;
import com.yas.order.viewmodel.product.ProductCheckoutListVm;
import com.yas.order.viewmodel.product.ProductGetCheckoutListVm;
import com.yas.order.viewmodel.product.ProductVariationVm;
import java.math.BigDecimal;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@SuppressWarnings({"rawtypes", "unchecked"})
class ProductServiceTest {

    private RestClient restClient;
    private ServiceUrlConfig serviceUrlConfig;
    private ProductService productService;
    private RestClient.ResponseSpec responseSpec;

    // Raw type to avoid wildcard capture mismatch with Mockito's thenReturn
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;

    private static final String PRODUCT_URL = "http://api.yas.local/product";
    private static final Long PRODUCT_ID = 1L;

    @BeforeEach
    void setUp() {
        restClient = mock(RestClient.class);
        serviceUrlConfig = mock(ServiceUrlConfig.class);
        productService = new ProductService(restClient, serviceUrlConfig);
        responseSpec = mock(RestClient.ResponseSpec.class);
        requestHeadersUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        setUpSecurityContext("test");
        when(serviceUrlConfig.product()).thenReturn(PRODUCT_URL);
    }

    // ──────────────────────────────────────────────────────────────────
    // getProductVariations
    // ──────────────────────────────────────────────────────────────────

    @Test
    void testGetProductVariations_whenValidProductId_shouldReturnList() {
        // Arrange
        List<ProductVariationVm> expected = List.of(
            new ProductVariationVm(1L, "Variation A", "SKU-A"),
            new ProductVariationVm(2L, "Variation B", "SKU-B")
        );

        URI url = UriComponentsBuilder
            .fromUriString(PRODUCT_URL)
            .path("/backoffice/product-variations/{productId}")
            .buildAndExpand(PRODUCT_ID)
            .toUri();

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(url)).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.headers(any())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);

        ResponseEntity<List<ProductVariationVm>> responseEntity = mock(ResponseEntity.class);
        when(responseSpec.toEntity(any(ParameterizedTypeReference.class))).thenReturn(responseEntity);
        when(responseEntity.getBody()).thenReturn(expected);

        // Act
        List<ProductVariationVm> result = productService.getProductVariations(PRODUCT_ID);

        // Assert
        assertThat(result).isNotNull().hasSize(2);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(0).sku()).isEqualTo("SKU-A");
    }

    @Test
    void testGetProductVariations_whenApiReturnsNull_shouldReturnNull() {
        // Arrange
        URI url = UriComponentsBuilder
            .fromUriString(PRODUCT_URL)
            .path("/backoffice/product-variations/{productId}")
            .buildAndExpand(PRODUCT_ID)
            .toUri();

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(url)).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.headers(any())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);

        ResponseEntity<List<ProductVariationVm>> responseEntity = mock(ResponseEntity.class);
        when(responseSpec.toEntity(any(ParameterizedTypeReference.class))).thenReturn(responseEntity);
        when(responseEntity.getBody()).thenReturn(null);

        // Act
        List<ProductVariationVm> result = productService.getProductVariations(PRODUCT_ID);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    void testHandleProductVariationListFallback_whenThrowable_shouldRethrow() {
        RuntimeException cause = new RuntimeException("circuit open");

        assertThatThrownBy(() -> productService.handleProductVariationListFallback(cause))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("circuit open");
    }

    // ──────────────────────────────────────────────────────────────────
    // subtractProductStockQuantity
    // ──────────────────────────────────────────────────────────────────

    @Test
    void testSubtractProductStockQuantity_whenValidOrderVm_shouldNotThrow() {
        OrderVm orderVm = buildOrderVm();

        URI url = UriComponentsBuilder
            .fromUriString(PRODUCT_URL)
            .path("/backoffice/products/subtract-quantity")
            .buildAndExpand()
            .toUri();

        RestClient.RequestBodyUriSpec requestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        when(restClient.put()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(url)).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.headers(any())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.body(any())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.retrieve()).thenReturn(responseSpec);

        assertDoesNotThrow(() -> productService.subtractProductStockQuantity(orderVm));
    }

    @Test
    void testSubtractProductStockQuantity_whenOrderItemsEmpty_shouldNotThrow() {
        OrderVm orderVm = new OrderVm(
            1L, "test@example.com", null, null, null,
            0f, 0f, 0, BigDecimal.ZERO, BigDecimal.ZERO, null,
            OrderStatus.PENDING, DeliveryMethod.GRAB_EXPRESS,
            DeliveryStatus.PREPARING, PaymentStatus.PENDING,
            new HashSet<>(), UUID.randomUUID().toString()
        );

        URI url = UriComponentsBuilder
            .fromUriString(PRODUCT_URL)
            .path("/backoffice/products/subtract-quantity")
            .buildAndExpand()
            .toUri();

        RestClient.RequestBodyUriSpec requestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        when(restClient.put()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(url)).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.headers(any())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.body(any())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.retrieve()).thenReturn(responseSpec);

        assertDoesNotThrow(() -> productService.subtractProductStockQuantity(orderVm));
    }

    @Test
    void testHandleBodilessFallback_whenThrowable_shouldRethrow() {
        RuntimeException cause = new RuntimeException("service unavailable");

        assertThatThrownBy(() -> productService.handleBodilessFallback(cause))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("service unavailable");
    }

    // ──────────────────────────────────────────────────────────────────
    // getProductInfomation
    // ──────────────────────────────────────────────────────────────────

    @Test
    void testGetProductInfomation_whenValidResponse_shouldReturnMappedResult() {
        Set<Long> ids = Set.of(1L, 2L);
        ProductCheckoutListVm vm1 = new ProductCheckoutListVm(1L, "Product A", 10.0, 100L);
        ProductCheckoutListVm vm2 = new ProductCheckoutListVm(2L, "Product B", 20.0, 200L);
        ProductGetCheckoutListVm response = new ProductGetCheckoutListVm(List.of(vm1, vm2), 0, 10, 2, 1, true);

        mockGetProductInfomationRestClient(ids, 0, 10, response);

        Map<Long, ProductCheckoutListVm> result = productService.getProductInfomation(ids, 0, 10);

        assertThat(result).isNotNull().hasSize(2);
        assertThat(result.get(1L).getName()).isEqualTo("Product A");
        assertThat(result.get(2L).getName()).isEqualTo("Product B");
    }

    @Test
    void testGetProductInfomation_whenResponseIsNull_shouldThrowNotFoundException() {
        Set<Long> ids = Set.of(1L);
        mockGetProductInfomationRestClient(ids, 0, 10, null);

        assertThatThrownBy(() -> productService.getProductInfomation(ids, 0, 10))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("PRODUCT_NOT_FOUND");
    }

    @Test
    void testGetProductInfomation_whenProductListIsNull_shouldThrowNotFoundException() {
        Set<Long> ids = Set.of(1L);
        ProductGetCheckoutListVm response = new ProductGetCheckoutListVm(null, 0, 10, 0, 0, true);
        mockGetProductInfomationRestClient(ids, 0, 10, response);

        assertThatThrownBy(() -> productService.getProductInfomation(ids, 0, 10))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("PRODUCT_NOT_FOUND");
    }

    @Test
    void testGetProductInfomation_whenProductListEmpty_shouldReturnEmptyMap() {
        Set<Long> ids = Set.of();
        ProductGetCheckoutListVm response = new ProductGetCheckoutListVm(List.of(), 0, 10, 0, 0, true);
        mockGetProductInfomationRestClient(ids, 0, 10, response);

        Map<Long, ProductCheckoutListVm> result = productService.getProductInfomation(ids, 0, 10);

        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    void testHandleProductInfomationFallback_whenThrowable_shouldRethrow() {
        RuntimeException cause = new RuntimeException("timeout");

        assertThatThrownBy(() -> productService.handleProductInfomationFallback(cause))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("timeout");
    }

    // ──────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────

    /**
     * Builds the RestClient mock chain for getProductInfomation.
     * Uses a single shared requestHeadersUriSpec (raw type) to avoid wildcard capture errors.
     */
    private void mockGetProductInfomationRestClient(
        Set<Long> ids, int pageNo, int pageSize, ProductGetCheckoutListVm responseBody
    ) {
        URI url = UriComponentsBuilder
            .fromUriString(PRODUCT_URL)
            .path("/products")
            .queryParam("ids", ids)
            .queryParam("pageNo", pageNo)
            .queryParam("pageSize", pageSize)
            .buildAndExpand()
            .toUri();

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(url)).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.headers(any())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);

        ResponseEntity<ProductGetCheckoutListVm> responseEntity = mock(ResponseEntity.class);
        when(responseSpec.toEntity(any(ParameterizedTypeReference.class))).thenReturn(responseEntity);
        when(responseEntity.getBody()).thenReturn(responseBody);
    }

    private static @NotNull OrderVm buildOrderVm() {
        Set<OrderItemVm> items = new HashSet<>();
        items.add(new OrderItemVm(
            1L, 101L, "Product A", 2, new BigDecimal("29.99"),
            "Note A", new BigDecimal("5.00"), new BigDecimal("2.00"),
            new BigDecimal("6.67"), 1001L
        ));
        items.add(new OrderItemVm(
            2L, 102L, "Product B", 1, new BigDecimal("49.99"),
            "Note B", new BigDecimal("10.00"), new BigDecimal("5.00"),
            new BigDecimal("10.00"), 1001L
        ));

        return new OrderVm(
            1L, "customer@example.com", null, null, "Deliver 9-5",
            5.0f, 10.0f, 3, new BigDecimal("89.97"), new BigDecimal("5.00"),
            "COUPON2024", OrderStatus.PENDING, DeliveryMethod.GRAB_EXPRESS,
            DeliveryStatus.PREPARING, PaymentStatus.PENDING,
            items, UUID.randomUUID().toString()
        );
    }
}
