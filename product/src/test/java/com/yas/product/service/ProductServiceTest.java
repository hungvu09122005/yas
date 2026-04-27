package com.yas.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.BadRequestException;
import com.yas.commonlibrary.exception.DuplicatedException;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.product.model.Brand;
import com.yas.product.model.Category;
import com.yas.product.model.Product;
import com.yas.product.model.ProductCategory;
import com.yas.product.model.ProductImage;
import com.yas.product.model.ProductRelated;
import com.yas.product.model.enumeration.DimensionUnit;
import com.yas.product.repository.BrandRepository;
import com.yas.product.repository.CategoryRepository;
import com.yas.product.repository.ProductCategoryRepository;
import com.yas.product.repository.ProductImageRepository;
import com.yas.product.repository.ProductOptionCombinationRepository;
import com.yas.product.repository.ProductOptionRepository;
import com.yas.product.repository.ProductOptionValueRepository;
import com.yas.product.repository.ProductRelatedRepository;
import com.yas.product.repository.ProductRepository;
import com.yas.product.viewmodel.NoFileMediaVm;
import com.yas.product.viewmodel.product.ProductDetailVm;
import com.yas.product.viewmodel.product.ProductEsDetailVm;
import com.yas.product.viewmodel.product.ProductListGetVm;
import com.yas.product.viewmodel.product.ProductListVm;
import com.yas.product.viewmodel.product.ProductPostVm;
import com.yas.product.viewmodel.product.ProductSlugGetVm;
import com.yas.product.viewmodel.product.ProductThumbnailVm;
import com.yas.product.viewmodel.product.ProductsGetVm;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    // ── Constants ─────────────────────────────────────────────────────────────

    private static final Long PRODUCT_ID = 1L;
    private static final Long BRAND_ID = 10L;
    private static final Long CATEGORY_ID = 20L;
    private static final String PRODUCT_NAME = "Test Product";
    private static final String PRODUCT_SLUG = "test-product";
    private static final String PRODUCT_SKU = "SKU-001";
    private static final String PRODUCT_GTIN = "GTIN-001";
    private static final String BRAND_SLUG = "brand-slug";
    private static final String CATEGORY_SLUG = "category-slug";
    private static final String MEDIA_URL = "http://media/1";

    // ── Mocks ─────────────────────────────────────────────────────────────────

    @Mock private ProductRepository productRepository;
    @Mock private MediaService mediaService;
    @Mock private BrandRepository brandRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private ProductCategoryRepository productCategoryRepository;
    @Mock private ProductImageRepository productImageRepository;
    @Mock private ProductOptionRepository productOptionRepository;
    @Mock private ProductOptionValueRepository productOptionValueRepository;
    @Mock private ProductOptionCombinationRepository productOptionCombinationRepository;
    @Mock private ProductRelatedRepository productRelatedRepository;

    @InjectMocks
    private ProductService productService;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Brand buildBrand(Long id) {
        Brand brand = new Brand();
        brand.setId(id);
        brand.setName("Brand");
        brand.setSlug(BRAND_SLUG);
        return brand;
    }

    private Category buildCategory(Long id) {
        Category cat = new Category();
        cat.setId(id);
        cat.setName("Category");
        cat.setSlug(CATEGORY_SLUG);
        return cat;
    }

    private Product buildProduct(Long id) {
        return Product.builder()
            .id(id)
            .name(PRODUCT_NAME)
            .slug(PRODUCT_SLUG)
            .sku(PRODUCT_SKU)
            .gtin(PRODUCT_GTIN)
            .price(100.0)
            .length(10.0)
            .width(5.0)
            .height(5.0)
            .dimensionUnit(DimensionUnit.CM)
            .isPublished(true)
            .isFeatured(false)
            .isAllowedToOrder(true)
            .isVisibleIndividually(true)
            .stockTrackingEnabled(true)
            .thumbnailMediaId(1L)
            .productCategories(new ArrayList<>())
            .productImages(new ArrayList<>())
            .attributeValues(new ArrayList<>())
            .relatedProducts(new ArrayList<>())
            .products(new ArrayList<>())
            .build();
    }

    private NoFileMediaVm mediaVm() {
        return new NoFileMediaVm(1L, "caption", "file.jpg", "image/jpeg", MEDIA_URL);
    }

    // ── getProductById ────────────────────────────────────────────────────────

    @Nested
    class GetProductByIdTest {

        @Test
        void testGetProductById_whenProductExists_shouldReturnProductDetailVm() {
            Product product = buildProduct(PRODUCT_ID);
            product.setBrand(buildBrand(BRAND_ID));
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
            when(mediaService.getMedia(anyLong())).thenReturn(mediaVm());

            ProductDetailVm result = productService.getProductById(PRODUCT_ID);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(PRODUCT_ID);
            assertThat(result.name()).isEqualTo(PRODUCT_NAME);
            assertThat(result.brandId()).isEqualTo(BRAND_ID);
        }

        @Test
        void testGetProductById_whenProductNotFound_shouldThrowNotFoundException() {
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.getProductById(PRODUCT_ID))
                .isInstanceOf(NotFoundException.class);
        }

        @Test
        void testGetProductById_whenProductHasNoThumbnail_shouldReturnNullThumbnailMedia() {
            Product product = buildProduct(PRODUCT_ID);
            product.setThumbnailMediaId(null);
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

            ProductDetailVm result = productService.getProductById(PRODUCT_ID);

            assertThat(result.thumbnailMedia()).isNull();
            verify(mediaService, never()).getMedia(null);
        }

        @Test
        void testGetProductById_whenProductHasImages_shouldReturnImageMediaList() {
            Product product = buildProduct(PRODUCT_ID);
            ProductImage image = ProductImage.builder().imageId(2L).product(product).build();
            product.getProductImages().add(image);
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
            when(mediaService.getMedia(anyLong())).thenReturn(mediaVm());

            ProductDetailVm result = productService.getProductById(PRODUCT_ID);

            assertThat(result.productImageMedias()).hasSize(1);
        }
    }

    // ── getProductsWithFilter ─────────────────────────────────────────────────

    @Nested
    class GetProductsWithFilterTest {

        @Test
        void testGetProductsWithFilter_whenProductsExist_shouldReturnPagedResult() {
            Product product = buildProduct(PRODUCT_ID);
            Page<Product> productPage = new PageImpl<>(List.of(product));
            when(productRepository.getProductsWithFilter(anyString(), anyString(), any(Pageable.class)))
                .thenReturn(productPage);

            ProductListGetVm result = productService.getProductsWithFilter(0, 10, "test", "brand");

            assertThat(result.productContent()).hasSize(1);
            assertThat(result.totalElements()).isEqualTo(1);
        }

        @Test
        void testGetProductsWithFilter_whenNoProducts_shouldReturnEmptyPage() {
            Page<Product> emptyPage = new PageImpl<>(Collections.emptyList());
            when(productRepository.getProductsWithFilter(anyString(), anyString(), any(Pageable.class)))
                .thenReturn(emptyPage);

            ProductListGetVm result = productService.getProductsWithFilter(0, 10, "", "");

            assertThat(result.productContent()).isEmpty();
            assertThat(result.totalElements()).isZero();
        }
    }

    // ── getLatestProducts ─────────────────────────────────────────────────────

    @Nested
    class GetLatestProductsTest {

        @Test
        void testGetLatestProducts_whenCountIsPositive_shouldReturnList() {
            Product product = buildProduct(PRODUCT_ID);
            when(productRepository.getLatestProducts(any(Pageable.class))).thenReturn(List.of(product));

            List<ProductListVm> result = productService.getLatestProducts(5);

            assertThat(result).hasSize(1);
        }

        @Test
        void testGetLatestProducts_whenCountIsZero_shouldReturnEmptyList() {
            List<ProductListVm> result = productService.getLatestProducts(0);

            assertThat(result).isEmpty();
            verify(productRepository, never()).getLatestProducts(any());
        }

        @Test
        void testGetLatestProducts_whenCountIsNegative_shouldReturnEmptyList() {
            List<ProductListVm> result = productService.getLatestProducts(-1);

            assertThat(result).isEmpty();
        }

        @Test
        void testGetLatestProducts_whenNoProducts_shouldReturnEmptyList() {
            when(productRepository.getLatestProducts(any(Pageable.class))).thenReturn(Collections.emptyList());

            List<ProductListVm> result = productService.getLatestProducts(3);

            assertThat(result).isEmpty();
        }
    }

    // ── getProductsByBrand ────────────────────────────────────────────────────

    @Nested
    class GetProductsByBrandTest {

        @Test
        void testGetProductsByBrand_whenBrandExists_shouldReturnThumbnailVms() {
            Brand brand = buildBrand(BRAND_ID);
            Product product = buildProduct(PRODUCT_ID);
            when(brandRepository.findBySlug(BRAND_SLUG)).thenReturn(Optional.of(brand));
            when(productRepository.findAllByBrandAndIsPublishedTrueOrderByIdAsc(brand)).thenReturn(List.of(product));
            when(mediaService.getMedia(anyLong())).thenReturn(mediaVm());

            List<ProductThumbnailVm> result = productService.getProductsByBrand(BRAND_SLUG);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).name()).isEqualTo(PRODUCT_NAME);
        }

        @Test
        void testGetProductsByBrand_whenBrandNotFound_shouldThrowNotFoundException() {
            when(brandRepository.findBySlug(BRAND_SLUG)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.getProductsByBrand(BRAND_SLUG))
                .isInstanceOf(NotFoundException.class);
        }

        @Test
        void testGetProductsByBrand_whenBrandHasNoProducts_shouldReturnEmptyList() {
            Brand brand = buildBrand(BRAND_ID);
            when(brandRepository.findBySlug(BRAND_SLUG)).thenReturn(Optional.of(brand));
            when(productRepository.findAllByBrandAndIsPublishedTrueOrderByIdAsc(brand))
                .thenReturn(Collections.emptyList());

            List<ProductThumbnailVm> result = productService.getProductsByBrand(BRAND_SLUG);

            assertThat(result).isEmpty();
        }
    }

    // ── deleteProduct ─────────────────────────────────────────────────────────

    @Nested
    class DeleteProductTest {

        @Test
        void testDeleteProduct_whenProductExists_shouldUnpublishAndSave() {
            Product product = buildProduct(PRODUCT_ID);
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
            when(productRepository.save(any(Product.class))).thenReturn(product);

            productService.deleteProduct(PRODUCT_ID);

            assertThat(product.isPublished()).isFalse();
            verify(productRepository).save(product);
        }

        @Test
        void testDeleteProduct_whenProductNotFound_shouldThrowNotFoundException() {
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.deleteProduct(PRODUCT_ID))
                .isInstanceOf(NotFoundException.class);
        }

        @Test
        void testDeleteProduct_whenProductIsVariant_shouldDeleteOptionCombinations() {
            Product parent = buildProduct(99L);
            Product variant = buildProduct(PRODUCT_ID);
            variant.setParent(parent);

            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(variant));
            when(productOptionCombinationRepository.findAllByProduct(variant)).thenReturn(List.of());
            when(productRepository.save(any())).thenReturn(variant);

            productService.deleteProduct(PRODUCT_ID);

            verify(productOptionCombinationRepository).findAllByProduct(variant);
        }
    }

    // ── getProductSlug ────────────────────────────────────────────────────────

    @Nested
    class GetProductSlugTest {

        @Test
        void testGetProductSlug_whenProductIsTopLevel_shouldReturnProductSlug() {
            Product product = buildProduct(PRODUCT_ID);
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

            ProductSlugGetVm result = productService.getProductSlug(PRODUCT_ID);

            assertThat(result.slug()).isEqualTo(PRODUCT_SLUG);
            assertThat(result.productVariantId()).isNull();
        }

        @Test
        void testGetProductSlug_whenProductIsVariant_shouldReturnParentSlug() {
            Product parent = buildProduct(99L);
            parent.setSlug("parent-slug");
            Product variant = buildProduct(PRODUCT_ID);
            variant.setParent(parent);
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(variant));

            ProductSlugGetVm result = productService.getProductSlug(PRODUCT_ID);

            assertThat(result.slug()).isEqualTo("parent-slug");
            assertThat(result.productVariantId()).isEqualTo(PRODUCT_ID);
        }

        @Test
        void testGetProductSlug_whenProductNotFound_shouldThrowNotFoundException() {
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.getProductSlug(PRODUCT_ID))
                .isInstanceOf(NotFoundException.class);
        }
    }

    // ── getProductEsDetailById ────────────────────────────────────────────────

    @Nested
    class GetProductEsDetailByIdTest {

        @Test
        void testGetProductEsDetailById_whenProductExists_shouldReturnEsDetailVm() {
            Product product = buildProduct(PRODUCT_ID);
            product.setBrand(buildBrand(BRAND_ID));
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

            ProductEsDetailVm result = productService.getProductEsDetailById(PRODUCT_ID);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(PRODUCT_ID);
            assertThat(result.name()).isEqualTo(PRODUCT_NAME);
            assertThat(result.brand()).isEqualTo("Brand");
        }

        @Test
        void testGetProductEsDetailById_whenProductNotFound_shouldThrowNotFoundException() {
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.getProductEsDetailById(PRODUCT_ID))
                .isInstanceOf(NotFoundException.class);
        }

        @Test
        void testGetProductEsDetailById_whenProductHasNoBrand_shouldReturnNullBrandName() {
            Product product = buildProduct(PRODUCT_ID);
            // brand is null by default
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

            ProductEsDetailVm result = productService.getProductEsDetailById(PRODUCT_ID);

            assertThat(result.brand()).isNull();
        }
    }

    // ── getRelatedProductsBackoffice ──────────────────────────────────────────

    @Nested
    class GetRelatedProductsBackofficeTest {

        @Test
        void testGetRelatedProductsBackoffice_whenProductExists_shouldReturnRelatedList() {
            Product product = buildProduct(PRODUCT_ID);
            Product related = buildProduct(2L);
            ProductRelated productRelated = ProductRelated.builder()
                .product(product).relatedProduct(related).build();
            product.getRelatedProducts().add(productRelated);
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

            List<ProductListVm> result = productService.getRelatedProductsBackoffice(PRODUCT_ID);

            assertThat(result).hasSize(1);
        }

        @Test
        void testGetRelatedProductsBackoffice_whenProductNotFound_shouldThrowNotFoundException() {
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.getRelatedProductsBackoffice(PRODUCT_ID))
                .isInstanceOf(NotFoundException.class);
        }

        @Test
        void testGetRelatedProductsBackoffice_whenNoRelatedProducts_shouldReturnEmptyList() {
            Product product = buildProduct(PRODUCT_ID);
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

            List<ProductListVm> result = productService.getRelatedProductsBackoffice(PRODUCT_ID);

            assertThat(result).isEmpty();
        }
    }

    // ── getProductsByMultiQuery ───────────────────────────────────────────────

    @Nested
    class GetProductsByMultiQueryTest {

        @Test
        void testGetProductsByMultiQuery_whenResultsExist_shouldReturnPagedVm() {
            Product product = buildProduct(PRODUCT_ID);
            Page<Product> page = new PageImpl<>(List.of(product));
            when(productRepository.findByProductNameAndCategorySlugAndPriceBetween(
                anyString(), anyString(), any(), any(), any(Pageable.class)))
                .thenReturn(page);
            when(mediaService.getMedia(anyLong())).thenReturn(mediaVm());

            ProductsGetVm result = productService.getProductsByMultiQuery(0, 10, "test", "cat", 0.0, 999.0);

            assertThat(result.productContent()).hasSize(1);
        }

        @Test
        void testGetProductsByMultiQuery_whenNoResults_shouldReturnEmptyPage() {
            Page<Product> emptyPage = new PageImpl<>(Collections.emptyList());
            when(productRepository.findByProductNameAndCategorySlugAndPriceBetween(
                anyString(), anyString(), any(), any(), any(Pageable.class)))
                .thenReturn(emptyPage);

            ProductsGetVm result = productService.getProductsByMultiQuery(0, 10, "", "", null, null);

            assertThat(result.productContent()).isEmpty();
        }
    }

    // ── getProductByIds ───────────────────────────────────────────────────────

    @Nested
    class GetProductByIdsTest {

        @Test
        void testGetProductByIds_whenIdsExist_shouldReturnListVm() {
            Product product = buildProduct(PRODUCT_ID);
            when(productRepository.findAllByIdIn(List.of(PRODUCT_ID))).thenReturn(List.of(product));

            List<ProductListVm> result = productService.getProductByIds(List.of(PRODUCT_ID));

            assertThat(result).hasSize(1);
        }

        @Test
        void testGetProductByIds_whenNoMatchingIds_shouldReturnEmptyList() {
            when(productRepository.findAllByIdIn(anyList())).thenReturn(Collections.emptyList());

            List<ProductListVm> result = productService.getProductByIds(List.of(999L));

            assertThat(result).isEmpty();
        }
    }

    // ── getProductByCategoryIds / getProductByBrandIds ────────────────────────

    @Nested
    class GetProductByCategoryIdsTest {

        @Test
        void testGetProductByCategoryIds_whenProductsExist_shouldReturnList() {
            Product product = buildProduct(PRODUCT_ID);
            when(productRepository.findByCategoryIdsIn(List.of(CATEGORY_ID))).thenReturn(List.of(product));

            List<ProductListVm> result = productService.getProductByCategoryIds(List.of(CATEGORY_ID));

            assertThat(result).hasSize(1);
        }

        @Test
        void testGetProductByCategoryIds_whenNoProducts_shouldReturnEmptyList() {
            when(productRepository.findByCategoryIdsIn(anyList())).thenReturn(Collections.emptyList());

            List<ProductListVm> result = productService.getProductByCategoryIds(List.of(99L));

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class GetProductByBrandIdsTest {

        @Test
        void testGetProductByBrandIds_whenProductsExist_shouldReturnList() {
            Product product = buildProduct(PRODUCT_ID);
            when(productRepository.findByBrandIdsIn(List.of(BRAND_ID))).thenReturn(List.of(product));

            List<ProductListVm> result = productService.getProductByBrandIds(List.of(BRAND_ID));

            assertThat(result).hasSize(1);
        }
    }

    // ── createProduct (validation paths) ─────────────────────────────────────

    @Nested
    class CreateProductValidationTest {

        private ProductPostVm buildValidPostVm() {
            return new ProductPostVm(
                PRODUCT_NAME,          // name
                PRODUCT_SLUG,          // slug
                BRAND_ID,              // brandId
                List.of(CATEGORY_ID),  // categoryIds
                "Short desc",          // shortDescription
                "Desc",                // description
                "Spec",                // specification
                PRODUCT_SKU,           // sku
                PRODUCT_GTIN,          // gtin
                10.0,                  // weight
                DimensionUnit.CM,      // dimensionUnit
                10.0,                  // length
                5.0,                   // width
                5.0,                   // height
                100.0,                 // price
                false,                 // isAllowedToOrder
                true,                  // isPublished
                false,                 // isFeatured
                true,                  // isVisibleIndividually
                true,                  // stockTrackingEnabled
                "meta title",          // metaTitle
                "meta kw",             // metaKeyword
                "meta desc",           // metaDescription
                1L,                    // thumbnailMediaId
                List.of(),             // productImageIds
                List.of(),             // variations
                List.of(),             // productOptionValues
                List.of(),             // productOptionValueDisplays
                List.of(),             // relatedProductIds
                1L                     // taxClassId
            );
        }

        @Test
        void testCreateProduct_whenLengthLessThanWidth_shouldThrowBadRequestException() {
            ProductPostVm postVm = new ProductPostVm(
                PRODUCT_NAME, PRODUCT_SLUG, BRAND_ID, List.of(),
                "s", "d", "sp", PRODUCT_SKU, PRODUCT_GTIN,
                10.0, DimensionUnit.CM,
                3.0,  // length ← less than width
                5.0,  // width
                5.0, 100.0,
                false, true, false, true, true,
                "mt", "mk", "md",
                1L, List.of(), List.of(), List.of(), List.of(), List.of(), 1L
            );

            assertThatThrownBy(() -> productService.createProduct(postVm))
                .isInstanceOf(BadRequestException.class);
        }

        @Test
        void testCreateProduct_whenSlugAlreadyExists_shouldThrowDuplicatedException() {
            ProductPostVm postVm = buildValidPostVm();
            Product existing = buildProduct(99L);
            when(productRepository.findBySlugAndIsPublishedTrue(PRODUCT_SLUG.toLowerCase()))
                .thenReturn(Optional.of(existing));
            // findAllById is NOT stubbed: the slug-duplicate exception is thrown inside
            // validateExistingProductProperties(), which runs BEFORE the findAllById call
            // in validateProductVm(), so findAllById is never reached.

            assertThatThrownBy(() -> productService.createProduct(postVm))
                .isInstanceOf(DuplicatedException.class);
        }

        @Test
        void testCreateProduct_whenSkuAlreadyExists_shouldThrowDuplicatedException() {
            ProductPostVm postVm = buildValidPostVm();
            Product existing = buildProduct(99L);
            when(productRepository.findBySlugAndIsPublishedTrue(anyString())).thenReturn(Optional.empty());
            when(productRepository.findByGtinAndIsPublishedTrue(any())).thenReturn(Optional.empty());
            when(productRepository.findBySkuAndIsPublishedTrue(PRODUCT_SKU)).thenReturn(Optional.of(existing));
            // findAllById is NOT stubbed: the SKU-duplicate exception is thrown inside
            // validateExistingProductProperties(), which runs BEFORE the findAllById call
            // in validateProductVm(), so findAllById is never reached.

            assertThatThrownBy(() -> productService.createProduct(postVm))
                .isInstanceOf(DuplicatedException.class);
        }

        @Test
        void testCreateProduct_whenValidRequestWithNoVariations_shouldSaveAndReturnVm() {
            ProductPostVm postVm = buildValidPostVm();
            Brand brand = buildBrand(BRAND_ID);
            Category category = buildCategory(CATEGORY_ID);
            Product saved = buildProduct(PRODUCT_ID);

            when(productRepository.findBySlugAndIsPublishedTrue(anyString())).thenReturn(Optional.empty());
            when(productRepository.findByGtinAndIsPublishedTrue(anyString())).thenReturn(Optional.empty());
            when(productRepository.findBySkuAndIsPublishedTrue(anyString())).thenReturn(Optional.empty());
            when(productRepository.findAllById(anyList())).thenReturn(Collections.emptyList());
            when(brandRepository.findById(BRAND_ID)).thenReturn(Optional.of(brand));
            when(productRepository.save(any(Product.class))).thenReturn(saved);
            when(categoryRepository.findAllById(List.of(CATEGORY_ID))).thenReturn(List.of(category));
            when(productImageRepository.saveAll(anyList())).thenReturn(Collections.emptyList());
            when(productCategoryRepository.saveAll(anyList())).thenReturn(Collections.emptyList());

            var result = productService.createProduct(postVm);

            assertThat(result).isNotNull();
            verify(productRepository).save(any(Product.class));
        }
    }

    // ── setProductImages ──────────────────────────────────────────────────────

    @Nested
    class SetProductImagesTest {

        @Test
        void testSetProductImages_whenImageIdsIsEmpty_shouldDeleteExistingAndReturnEmpty() {
            Product product = buildProduct(PRODUCT_ID);
            product.setId(PRODUCT_ID);

            List<ProductImage> result = productService.setProductImages(Collections.emptyList(), product);

            assertThat(result).isEmpty();
            verify(productImageRepository).deleteByProductId(PRODUCT_ID);
        }

        @Test
        void testSetProductImages_whenProductImagesIsNull_shouldCreateNewImages() {
            Product product = buildProduct(PRODUCT_ID);
            product.setProductImages(null);

            List<ProductImage> result = productService.setProductImages(List.of(100L, 200L), product);

            assertThat(result).hasSize(2);
        }

        @Test
        void testSetProductImages_whenProductAlreadyHasSameImages_shouldReturnEmpty() {
            Product product = buildProduct(PRODUCT_ID);
            ProductImage existingImage = ProductImage.builder().imageId(100L).product(product).build();
            product.getProductImages().add(existingImage);

            // Same IDs as existing → nothing new, nothing deleted
            List<ProductImage> result = productService.setProductImages(List.of(100L), product);

            assertThat(result).isEmpty();
        }
    }

    // ── exportProducts ────────────────────────────────────────────────────────

    @Nested
    class ExportProductsTest {

        @Test
        void testExportProducts_whenProductsExist_shouldReturnDetailList() {
            Product product = buildProduct(PRODUCT_ID);
            Brand brand = buildBrand(BRAND_ID);
            product.setBrand(brand);
            when(productRepository.getExportingProducts(anyString(), anyString())).thenReturn(List.of(product));

            var result = productService.exportProducts("test", "brand");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).name()).isEqualTo(PRODUCT_NAME);
        }

        @Test
        void testExportProducts_whenNoProducts_shouldReturnEmptyList() {
            when(productRepository.getExportingProducts(anyString(), anyString()))
                .thenReturn(Collections.emptyList());

            var result = productService.exportProducts("", "");

            assertThat(result).isEmpty();
        }
    }

    // ── getProductVariationsByParentId ────────────────────────────────────────

    @Nested
    class GetProductVariationsByParentIdTest {

        @Test
        void testGetProductVariationsByParentId_whenParentHasNoOptions_shouldReturnEmptyList() {
            Product parent = buildProduct(PRODUCT_ID);
            parent.setHasOptions(false);
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(parent));

            var result = productService.getProductVariationsByParentId(PRODUCT_ID);

            assertThat(result).isEmpty();
        }

        @Test
        void testGetProductVariationsByParentId_whenParentNotFound_shouldThrowNotFoundException() {
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.getProductVariationsByParentId(PRODUCT_ID))
                .isInstanceOf(NotFoundException.class);
        }

        @Test
        void testGetProductVariationsByParentId_whenParentHasOptionsAndPublishedVariant_shouldReturnVariations() {
            Product parent = buildProduct(PRODUCT_ID);
            parent.setHasOptions(true);
            Product variant = buildProduct(2L);
            variant.setThumbnailMediaId(null); // no thumbnail
            parent.getProducts().add(variant);

            com.yas.product.model.ProductOption option = new com.yas.product.model.ProductOption();
            option.setId(100L);
            com.yas.product.model.ProductOptionCombination combo = new com.yas.product.model.ProductOptionCombination();
            combo.setProductOption(option);
            combo.setValue("Blue");

            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(parent));
            when(productOptionCombinationRepository.findAllByProduct(variant)).thenReturn(List.of(combo));

            var result = productService.getProductVariationsByParentId(PRODUCT_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).options()).containsEntry(100L, "Blue");
        }
    }

    // ── subtractStockQuantity ─────────────────────────────────────────────────

    @Nested
    class SubtractStockQuantityTest {

        @Test
        void testSubtractStockQuantity_whenStockIsEnough_shouldReduceStock() {
            Product product = buildProduct(PRODUCT_ID);
            product.setStockQuantity(100L);
            product.setStockTrackingEnabled(true);

            when(productRepository.findAllByIdIn(List.of(PRODUCT_ID))).thenReturn(List.of(product));

            var items = List.of(new com.yas.product.viewmodel.product.ProductQuantityPutVm(PRODUCT_ID, 30L));
            productService.subtractStockQuantity(items);

            assertThat(product.getStockQuantity()).isEqualTo(70L);
            verify(productRepository).saveAll(List.of(product));
        }

        @Test
        void testSubtractStockQuantity_whenStockUnderflows_shouldClampToZero() {
            Product product = buildProduct(PRODUCT_ID);
            product.setStockQuantity(10L);
            product.setStockTrackingEnabled(true);

            when(productRepository.findAllByIdIn(List.of(PRODUCT_ID))).thenReturn(List.of(product));

            var items = List.of(new com.yas.product.viewmodel.product.ProductQuantityPutVm(PRODUCT_ID, 50L));
            productService.subtractStockQuantity(items);

            assertThat(product.getStockQuantity()).isEqualTo(0L);
        }

        @Test
        void testSubtractStockQuantity_whenStockTrackingDisabled_shouldNotChangeStock() {
            Product product = buildProduct(PRODUCT_ID);
            product.setStockQuantity(100L);
            product.setStockTrackingEnabled(false);

            when(productRepository.findAllByIdIn(List.of(PRODUCT_ID))).thenReturn(List.of(product));

            var items = List.of(new com.yas.product.viewmodel.product.ProductQuantityPutVm(PRODUCT_ID, 30L));
            productService.subtractStockQuantity(items);

            // stock tracking disabled → quantity unchanged
            assertThat(product.getStockQuantity()).isEqualTo(100L);
        }

        @Test
        void testSubtractStockQuantity_whenDuplicateProductIds_shouldMergeQuantities() {
            Product product = buildProduct(PRODUCT_ID);
            product.setStockQuantity(100L);
            product.setStockTrackingEnabled(true);

            when(productRepository.findAllByIdIn(anyList())).thenReturn(List.of(product));

            // two entries for same product → merged to 40
            var items = List.of(
                new com.yas.product.viewmodel.product.ProductQuantityPutVm(PRODUCT_ID, 20L),
                new com.yas.product.viewmodel.product.ProductQuantityPutVm(PRODUCT_ID, 20L)
            );
            productService.subtractStockQuantity(items);

            assertThat(product.getStockQuantity()).isEqualTo(60L);
        }
    }

    // ── restoreStockQuantity ──────────────────────────────────────────────────

    @Nested
    class RestoreStockQuantityTest {

        @Test
        void testRestoreStockQuantity_whenTrackingEnabled_shouldAddToStock() {
            Product product = buildProduct(PRODUCT_ID);
            product.setStockQuantity(50L);
            product.setStockTrackingEnabled(true);

            when(productRepository.findAllByIdIn(List.of(PRODUCT_ID))).thenReturn(List.of(product));

            var items = List.of(new com.yas.product.viewmodel.product.ProductQuantityPutVm(PRODUCT_ID, 20L));
            productService.restoreStockQuantity(items);

            assertThat(product.getStockQuantity()).isEqualTo(70L);
            verify(productRepository).saveAll(List.of(product));
        }

        @Test
        void testRestoreStockQuantity_whenTrackingDisabled_shouldNotChangeStock() {
            Product product = buildProduct(PRODUCT_ID);
            product.setStockQuantity(50L);
            product.setStockTrackingEnabled(false);

            when(productRepository.findAllByIdIn(List.of(PRODUCT_ID))).thenReturn(List.of(product));

            var items = List.of(new com.yas.product.viewmodel.product.ProductQuantityPutVm(PRODUCT_ID, 20L));
            productService.restoreStockQuantity(items);

            assertThat(product.getStockQuantity()).isEqualTo(50L);
        }
    }

    // ── updateProductQuantity ─────────────────────────────────────────────────

    @Nested
    class UpdateProductQuantityTest {

        @Test
        void testUpdateProductQuantity_whenProductsExist_shouldSetStockQuantity() {
            Product product = buildProduct(PRODUCT_ID);
            product.setStockQuantity(10L);
            when(productRepository.findAllByIdIn(List.of(PRODUCT_ID))).thenReturn(List.of(product));

            var postVms = List.of(new com.yas.product.viewmodel.product.ProductQuantityPostVm(PRODUCT_ID, 99L));
            productService.updateProductQuantity(postVms);

            assertThat(product.getStockQuantity()).isEqualTo(99L);
            verify(productRepository).saveAll(anyList());
        }

        @Test
        void testUpdateProductQuantity_whenNoMatchingProduct_shouldNotChangeQuantity() {
            when(productRepository.findAllByIdIn(anyList())).thenReturn(Collections.emptyList());

            var postVms = List.of(new com.yas.product.viewmodel.product.ProductQuantityPostVm(99L, 50L));
            productService.updateProductQuantity(postVms);

            verify(productRepository).saveAll(Collections.emptyList());
        }
    }

    // ── getListFeaturedProducts ───────────────────────────────────────────────

    @Nested
    class GetListFeaturedProductsTest {

        @Test
        void testGetListFeaturedProducts_whenProductsExist_shouldReturnPagedResult() {
            Product product = buildProduct(PRODUCT_ID);
            Page<Product> page = new PageImpl<>(List.of(product));
            when(productRepository.getFeaturedProduct(any(Pageable.class))).thenReturn(page);
            when(mediaService.getMedia(anyLong())).thenReturn(mediaVm());

            var result = productService.getListFeaturedProducts(0, 10);

            assertThat(result.productList()).hasSize(1);
        }

        @Test
        void testGetListFeaturedProducts_whenNoProducts_shouldReturnEmpty() {
            when(productRepository.getFeaturedProduct(any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

            var result = productService.getListFeaturedProducts(0, 10);

            assertThat(result.productList()).isEmpty();
        }
    }

    // ── getRelatedProductsStorefront ──────────────────────────────────────────

    @Nested
    class GetRelatedProductsStorefrontTest {

        @Test
        void testGetRelatedProductsStorefront_whenRelatedProductsExist_shouldReturnPublishedOnes() {
            Product product = buildProduct(PRODUCT_ID);
            Product related = buildProduct(2L);
            related.setThumbnailMediaId(2L);

            com.yas.product.model.ProductRelated productRelated =
                com.yas.product.model.ProductRelated.builder()
                    .product(product).relatedProduct(related).build();

            Page<com.yas.product.model.ProductRelated> relatedPage =
                new PageImpl<>(List.of(productRelated));

            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
            when(productRelatedRepository.findAllByProduct(any(), any(Pageable.class)))
                .thenReturn(relatedPage);
            when(mediaService.getMedia(2L)).thenReturn(mediaVm());

            var result = productService.getRelatedProductsStorefront(PRODUCT_ID, 0, 10);

            assertThat(result.productContent()).hasSize(1);
        }

        @Test
        void testGetRelatedProductsStorefront_whenProductNotFound_shouldThrowNotFoundException() {
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.getRelatedProductsStorefront(PRODUCT_ID, 0, 10))
                .isInstanceOf(NotFoundException.class);
        }

        @Test
        void testGetRelatedProductsStorefront_whenRelatedProductIsUnpublished_shouldFilterItOut() {
            Product product = buildProduct(PRODUCT_ID);
            Product unpublished = buildProduct(2L);
            unpublished.setPublished(false);

            com.yas.product.model.ProductRelated rel =
                com.yas.product.model.ProductRelated.builder()
                    .product(product).relatedProduct(unpublished).build();

            Page<com.yas.product.model.ProductRelated> relatedPage = new PageImpl<>(List.of(rel));
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
            when(productRelatedRepository.findAllByProduct(any(), any(Pageable.class)))
                .thenReturn(relatedPage);

            var result = productService.getRelatedProductsStorefront(PRODUCT_ID, 0, 10);

            assertThat(result.productContent()).isEmpty();
        }
    }

    // ── getProductsForWarehouse ───────────────────────────────────────────────

    @Nested
    class GetProductsForWarehouseTest {

        @Test
        void testGetProductsForWarehouse_whenProductsMatch_shouldReturnInfoVms() {
            Product product = buildProduct(PRODUCT_ID);
            when(productRepository.findProductForWarehouse(anyString(), anyString(), anyList(), anyString()))
                .thenReturn(List.of(product));

            var result = productService.getProductsForWarehouse(
                "name", "sku", List.of(PRODUCT_ID),
                com.yas.product.model.enumeration.FilterExistInWhSelection.ALL);

            assertThat(result).hasSize(1);
        }

        @Test
        void testGetProductsForWarehouse_whenNoMatch_shouldReturnEmpty() {
            when(productRepository.findProductForWarehouse(anyString(), anyString(), anyList(), anyString()))
                .thenReturn(Collections.emptyList());

            var result = productService.getProductsForWarehouse(
                "", "", List.of(),
                com.yas.product.model.enumeration.FilterExistInWhSelection.YES);

            assertThat(result).isEmpty();
        }
    }

    // ── getProductCheckoutList ────────────────────────────────────────────────

    @Nested
    class GetProductCheckoutListTest {

        @Test
        void testGetProductCheckoutList_whenProductsExist_shouldReturnVms() {
            Product product = buildProduct(PRODUCT_ID);
            Page<Product> page = new PageImpl<>(List.of(product));
            when(productRepository.findAllPublishedProductsByIds(anyList(), any(Pageable.class)))
                .thenReturn(page);
            when(mediaService.getMedia(anyLong())).thenReturn(mediaVm());

            var result = productService.getProductCheckoutList(0, 10, List.of(PRODUCT_ID));

            assertThat(result.productCheckoutListVms()).hasSize(1);
        }

        @Test
        void testGetProductCheckoutList_whenNoProducts_shouldReturnEmpty() {
            when(productRepository.findAllPublishedProductsByIds(anyList(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

            var result = productService.getProductCheckoutList(0, 10, List.of());

            assertThat(result.productCheckoutListVms()).isEmpty();
        }
    }
}

