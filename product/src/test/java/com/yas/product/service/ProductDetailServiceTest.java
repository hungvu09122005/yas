package com.yas.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.product.model.Brand;
import com.yas.product.model.Category;
import com.yas.product.model.Product;
import com.yas.product.model.ProductCategory;
import com.yas.product.model.ProductImage;
import com.yas.product.model.ProductOption;
import com.yas.product.model.ProductOptionCombination;
import com.yas.product.repository.ProductOptionCombinationRepository;
import com.yas.product.repository.ProductRepository;
import com.yas.product.viewmodel.NoFileMediaVm;
import com.yas.product.viewmodel.product.ProductDetailInfoVm;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductDetailServiceTest {

    private static final long PRODUCT_ID = 1L;
    private static final Long BRAND_ID = 10L;
    private static final String PRODUCT_NAME = "Test Product";
    private static final String PRODUCT_SLUG = "test-product";
    private static final String MEDIA_URL = "http://media/img.jpg";

    @Mock private ProductRepository productRepository;
    @Mock private MediaService mediaService;
    @Mock private ProductOptionCombinationRepository productOptionCombinationRepository;

    @InjectMocks
    private ProductDetailService productDetailService;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Product buildProduct(long id, boolean published, boolean hasOptions) {
        Product p = new Product();
        p.setId(id);
        p.setName(PRODUCT_NAME);
        p.setSlug(PRODUCT_SLUG);
        p.setPublished(published);
        p.setHasOptions(hasOptions);
        p.setProductCategories(new ArrayList<>());
        p.setAttributeValues(new ArrayList<>());
        p.setProductImages(new ArrayList<>());
        p.setProducts(new ArrayList<>());
        return p;
    }

    private NoFileMediaVm mediaVm() {
        return new NoFileMediaVm(1L, "cap", "img.jpg", "image/jpeg", MEDIA_URL);
    }

    // ── getProductDetailById ──────────────────────────────────────────────────

    @Nested
    class GetProductDetailByIdTest {

        @Test
        void testGetProductDetailById_whenProductIsPublished_shouldReturnDetailVm() {
            Product product = buildProduct(PRODUCT_ID, true, false);
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

            ProductDetailInfoVm result = productDetailService.getProductDetailById(PRODUCT_ID);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(PRODUCT_ID);
            assertThat(result.getName()).isEqualTo(PRODUCT_NAME);
        }

        @Test
        void testGetProductDetailById_whenProductNotFound_shouldThrowNotFoundException() {
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productDetailService.getProductDetailById(PRODUCT_ID))
                .isInstanceOf(NotFoundException.class);
        }

        @Test
        void testGetProductDetailById_whenProductIsUnpublished_shouldThrowNotFoundException() {
            Product product = buildProduct(PRODUCT_ID, false, false);
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

            assertThatThrownBy(() -> productDetailService.getProductDetailById(PRODUCT_ID))
                .isInstanceOf(NotFoundException.class);
        }

        @Test
        void testGetProductDetailById_whenProductHasBrand_shouldReturnBrandInfo() {
            Product product = buildProduct(PRODUCT_ID, true, false);
            Brand brand = new Brand();
            brand.setId(BRAND_ID);
            brand.setName("Nike");
            product.setBrand(brand);
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

            ProductDetailInfoVm result = productDetailService.getProductDetailById(PRODUCT_ID);

            assertThat(result.getBrandId()).isEqualTo(BRAND_ID);
            assertThat(result.getBrandName()).isEqualTo("Nike");
        }

        @Test
        void testGetProductDetailById_whenProductHasNoBrand_shouldReturnNullBrandFields() {
            Product product = buildProduct(PRODUCT_ID, true, false);
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

            ProductDetailInfoVm result = productDetailService.getProductDetailById(PRODUCT_ID);

            assertThat(result.getBrandId()).isNull();
            assertThat(result.getBrandName()).isNull();
        }

        @Test
        void testGetProductDetailById_whenProductHasThumbnail_shouldReturnThumbnailVm() {
            Product product = buildProduct(PRODUCT_ID, true, false);
            product.setThumbnailMediaId(1L);
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
            when(mediaService.getMedia(1L)).thenReturn(mediaVm());

            ProductDetailInfoVm result = productDetailService.getProductDetailById(PRODUCT_ID);

            assertThat(result.getThumbnail()).isNotNull();
            assertThat(result.getThumbnail().url()).isEqualTo(MEDIA_URL);
        }

        @Test
        void testGetProductDetailById_whenProductHasNoThumbnail_shouldReturnNullThumbnail() {
            Product product = buildProduct(PRODUCT_ID, true, false);
            product.setThumbnailMediaId(null);
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

            ProductDetailInfoVm result = productDetailService.getProductDetailById(PRODUCT_ID);

            assertThat(result.getThumbnail()).isNull();
        }

        @Test
        void testGetProductDetailById_whenProductHasImages_shouldReturnImageList() {
            Product product = buildProduct(PRODUCT_ID, true, false);
            ProductImage img = new ProductImage();
            img.setImageId(2L);
            img.setProduct(product);
            product.getProductImages().add(img);
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
            when(mediaService.getMedia(anyLong())).thenReturn(mediaVm());

            ProductDetailInfoVm result = productDetailService.getProductDetailById(PRODUCT_ID);

            assertThat(result.getProductImages()).hasSize(1);
        }

        @Test
        void testGetProductDetailById_whenProductHasNoImages_shouldReturnEmptyImageList() {
            Product product = buildProduct(PRODUCT_ID, true, false);
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

            ProductDetailInfoVm result = productDetailService.getProductDetailById(PRODUCT_ID);

            assertThat(result.getProductImages()).isEmpty();
        }

        @Test
        void testGetProductDetailById_whenProductHasCategories_shouldReturnCategoryList() {
            Product product = buildProduct(PRODUCT_ID, true, false);
            Category cat = new Category();
            cat.setId(5L);
            cat.setName("Electronics");
            ProductCategory pc = new ProductCategory();
            pc.setCategory(cat);
            pc.setProduct(product);
            product.getProductCategories().add(pc);
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

            ProductDetailInfoVm result = productDetailService.getProductDetailById(PRODUCT_ID);

            assertThat(result.getCategories()).hasSize(1);
            assertThat(result.getCategories().get(0).getName()).isEqualTo("Electronics");
        }

        @Test
        void testGetProductDetailById_whenProductHasOptionsAndPublishedVariant_shouldReturnVariations() {
            Product parent = buildProduct(PRODUCT_ID, true, true);
            Product variant = buildProduct(2L, true, false);
            parent.getProducts().add(variant);

            ProductOption option = new ProductOption();
            option.setId(100L);
            option.setName("Color");

            ProductOptionCombination combination = new ProductOptionCombination();
            combination.setProductOption(option);
            combination.setValue("Red");

            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(parent));
            when(productOptionCombinationRepository.findAllByProduct(variant))
                .thenReturn(List.of(combination));

            ProductDetailInfoVm result = productDetailService.getProductDetailById(PRODUCT_ID);

            assertThat(result.getVariations()).hasSize(1);
            assertThat(result.getVariations().get(0).id()).isEqualTo(2L);
            assertThat(result.getVariations().get(0).options()).containsEntry(100L, "Red");
        }

        @Test
        void testGetProductDetailById_whenProductHasOptionsButUnpublishedVariants_shouldReturnEmptyVariations() {
            Product parent = buildProduct(PRODUCT_ID, true, true);
            Product unpublishedVariant = buildProduct(2L, false, false);
            parent.getProducts().add(unpublishedVariant);
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(parent));

            ProductDetailInfoVm result = productDetailService.getProductDetailById(PRODUCT_ID);

            assertThat(result.getVariations()).isEmpty();
        }

        @Test
        void testGetProductDetailById_whenProductHasNoOptions_shouldReturnEmptyVariations() {
            Product product = buildProduct(PRODUCT_ID, true, false);
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

            ProductDetailInfoVm result = productDetailService.getProductDetailById(PRODUCT_ID);

            assertThat(result.getVariations()).isEmpty();
        }
    }
}
