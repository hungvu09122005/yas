package com.yas.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.BadRequestException;
import com.yas.commonlibrary.exception.DuplicatedException;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.product.model.Brand;
import com.yas.product.model.Product;
import com.yas.product.repository.BrandRepository;
import com.yas.product.viewmodel.brand.BrandListGetVm;
import com.yas.product.viewmodel.brand.BrandPostVm;
import com.yas.product.viewmodel.brand.BrandVm;
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
class BrandServiceTest {

    private static final Long BRAND_ID = 1L;
    private static final String BRAND_NAME = "Nike";
    private static final String BRAND_SLUG = "nike";

    @Mock
    private BrandRepository brandRepository;

    @InjectMocks
    private BrandService brandService;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Brand buildBrand(Long id, String name) {
        Brand brand = new Brand();
        brand.setId(id);
        brand.setName(name);
        brand.setSlug(BRAND_SLUG);
        brand.setPublished(true);
        brand.setProducts(Collections.emptyList());
        return brand;
    }

    private BrandPostVm buildPostVm(String name) {
        return new BrandPostVm(name, BRAND_SLUG, true);
    }

    // ── getBrands ─────────────────────────────────────────────────────────────

    @Nested
    class GetBrandsTest {

        @Test
        void testGetBrands_whenBrandsExist_shouldReturnPagedResult() {
            Brand brand = buildBrand(BRAND_ID, BRAND_NAME);
            Page<Brand> brandPage = new PageImpl<>(List.of(brand));
            when(brandRepository.findAll(any(Pageable.class))).thenReturn(brandPage);

            BrandListGetVm result = brandService.getBrands(0, 10);

            assertThat(result.brandContent()).hasSize(1);
            assertThat(result.brandContent().get(0).name()).isEqualTo(BRAND_NAME);
            assertThat(result.totalElements()).isEqualTo(1);
            assertThat(result.isLast()).isTrue();
        }

        @Test
        void testGetBrands_whenNoBrandsExist_shouldReturnEmptyPage() {
            Page<Brand> emptyPage = new PageImpl<>(Collections.emptyList());
            when(brandRepository.findAll(any(Pageable.class))).thenReturn(emptyPage);

            BrandListGetVm result = brandService.getBrands(0, 10);

            assertThat(result.brandContent()).isEmpty();
            assertThat(result.totalElements()).isZero();
        }
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Nested
    class CreateBrandTest {

        @Test
        void testCreate_whenNameIsUnique_shouldSaveAndReturnBrand() {
            BrandPostVm postVm = buildPostVm(BRAND_NAME);
            Brand saved = buildBrand(BRAND_ID, BRAND_NAME);
            when(brandRepository.findExistedName(BRAND_NAME, null)).thenReturn(null);
            when(brandRepository.save(any(Brand.class))).thenReturn(saved);

            Brand result = brandService.create(postVm);

            assertThat(result.getName()).isEqualTo(BRAND_NAME);
            verify(brandRepository).save(any(Brand.class));
        }

        @Test
        void testCreate_whenNameAlreadyExists_shouldThrowDuplicatedException() {
            BrandPostVm postVm = buildPostVm(BRAND_NAME);
            when(brandRepository.findExistedName(BRAND_NAME, null)).thenReturn(buildBrand(BRAND_ID, BRAND_NAME));

            assertThatThrownBy(() -> brandService.create(postVm))
                .isInstanceOf(DuplicatedException.class);
        }
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Nested
    class UpdateBrandTest {

        @Test
        void testUpdate_whenBrandExistsAndNameIsUnique_shouldUpdateAndReturn() {
            BrandPostVm postVm = buildPostVm("Adidas");
            Brand existing = buildBrand(BRAND_ID, BRAND_NAME);
            when(brandRepository.findExistedName("Adidas", BRAND_ID)).thenReturn(null);
            when(brandRepository.findById(BRAND_ID)).thenReturn(Optional.of(existing));
            when(brandRepository.save(any(Brand.class))).thenReturn(existing);

            Brand result = brandService.update(postVm, BRAND_ID);

            assertThat(result).isNotNull();
            verify(brandRepository).save(existing);
        }

        @Test
        void testUpdate_whenBrandNotFound_shouldThrowNotFoundException() {
            BrandPostVm postVm = buildPostVm(BRAND_NAME);
            when(brandRepository.findExistedName(BRAND_NAME, BRAND_ID)).thenReturn(null);
            when(brandRepository.findById(BRAND_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> brandService.update(postVm, BRAND_ID))
                .isInstanceOf(NotFoundException.class);
        }

        @Test
        void testUpdate_whenNameAlreadyExistsForOtherBrand_shouldThrowDuplicatedException() {
            BrandPostVm postVm = buildPostVm("Adidas");
            Brand otherBrand = buildBrand(2L, "Adidas");
            when(brandRepository.findExistedName("Adidas", BRAND_ID)).thenReturn(otherBrand);

            assertThatThrownBy(() -> brandService.update(postVm, BRAND_ID))
                .isInstanceOf(DuplicatedException.class);
        }
    }

    // ── getBrandsByIds ────────────────────────────────────────────────────────

    @Nested
    class GetBrandsByIdsTest {

        @Test
        void testGetBrandsByIds_whenBrandsExist_shouldReturnVmList() {
            Brand brand = buildBrand(BRAND_ID, BRAND_NAME);
            when(brandRepository.findAllById(List.of(BRAND_ID))).thenReturn(List.of(brand));

            List<BrandVm> result = brandService.getBrandsByIds(List.of(BRAND_ID));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).name()).isEqualTo(BRAND_NAME);
        }

        @Test
        void testGetBrandsByIds_whenNoMatchingIds_shouldReturnEmptyList() {
            when(brandRepository.findAllById(List.of(99L))).thenReturn(Collections.emptyList());

            List<BrandVm> result = brandService.getBrandsByIds(List.of(99L));

            assertThat(result).isEmpty();
        }
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Nested
    class DeleteBrandTest {

        @Test
        void testDelete_whenBrandExistsAndHasNoProducts_shouldDelete() {
            Brand brand = buildBrand(BRAND_ID, BRAND_NAME);
            when(brandRepository.findById(BRAND_ID)).thenReturn(Optional.of(brand));

            brandService.delete(BRAND_ID);

            verify(brandRepository).deleteById(BRAND_ID);
        }

        @Test
        void testDelete_whenBrandNotFound_shouldThrowNotFoundException() {
            when(brandRepository.findById(BRAND_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> brandService.delete(BRAND_ID))
                .isInstanceOf(NotFoundException.class);
        }

        @Test
        void testDelete_whenBrandHasProducts_shouldThrowBadRequestException() {
            Brand brand = buildBrand(BRAND_ID, BRAND_NAME);
            brand.setProducts(List.of(new Product()));
            when(brandRepository.findById(BRAND_ID)).thenReturn(Optional.of(brand));

            assertThatThrownBy(() -> brandService.delete(BRAND_ID))
                .isInstanceOf(BadRequestException.class);
        }
    }
}