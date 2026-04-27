package com.yas.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.BadRequestException;
import com.yas.commonlibrary.exception.DuplicatedException;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.product.model.Category;
import com.yas.product.repository.CategoryRepository;
import com.yas.product.viewmodel.NoFileMediaVm;
import com.yas.product.viewmodel.category.CategoryGetDetailVm;
import com.yas.product.viewmodel.category.CategoryGetVm;
import com.yas.product.viewmodel.category.CategoryListGetVm;
import com.yas.product.viewmodel.category.CategoryPostVm;
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

/**
 * Pure unit tests for CategoryService — replaces the former @SpringBootTest version.
 */
@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    private static final Long CATEGORY_ID = 1L;
    private static final Long PARENT_ID = 2L;
    private static final String CATEGORY_NAME = "Electronics";
    private static final String CATEGORY_SLUG = "electronics";
    private static final String MEDIA_URL = "http://media/img.jpg";

    @Mock private CategoryRepository categoryRepository;
    @Mock private MediaService mediaService;

    @InjectMocks
    private CategoryService categoryService;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Category buildCategory(Long id, String name) {
        Category c = new Category();
        c.setId(id);
        c.setName(name);
        c.setSlug(CATEGORY_SLUG);
        c.setIsPublished(true);
        c.setDisplayOrder((short) 1);
        return c;
    }

    private CategoryPostVm buildPostVm(String name, Long parentId) {
        return new CategoryPostVm(name, CATEGORY_SLUG, "desc", parentId,
            "metaKw", "metaDesc", (short) 1, true, 1L);
    }

    private NoFileMediaVm mediaVm() {
        return new NoFileMediaVm(1L, "cap", "img.jpg", "image/jpeg", MEDIA_URL);
    }

    // ── getPageableCategories ─────────────────────────────────────────────────

    @Nested
    class GetPageableCategoriesTest {

        @Test
        void testGetPageableCategories_whenCategoriesExist_shouldReturnPagedResult() {
            Category category = buildCategory(CATEGORY_ID, CATEGORY_NAME);
            Page<Category> page = new PageImpl<>(List.of(category));
            when(categoryRepository.findAll(any(Pageable.class))).thenReturn(page);

            CategoryListGetVm result = categoryService.getPageableCategories(0, 10);

            assertThat(result.categoryContent()).hasSize(1);
            assertThat(result.totalElements()).isEqualTo(1);
        }

        @Test
        void testGetPageableCategories_whenEmpty_shouldReturnEmptyPage() {
            when(categoryRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

            CategoryListGetVm result = categoryService.getPageableCategories(0, 10);

            assertThat(result.categoryContent()).isEmpty();
        }
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Nested
    class CreateCategoryTest {

        @Test
        void testCreate_whenNameIsUniqueAndNoParent_shouldSave() {
            CategoryPostVm postVm = buildPostVm(CATEGORY_NAME, null);
            Category saved = buildCategory(CATEGORY_ID, CATEGORY_NAME);
            when(categoryRepository.findExistedName(CATEGORY_NAME, null)).thenReturn(null);
            when(categoryRepository.save(any(Category.class))).thenReturn(saved);

            Category result = categoryService.create(postVm);

            assertThat(result.getName()).isEqualTo(CATEGORY_NAME);
            verify(categoryRepository).save(any(Category.class));
        }

        @Test
        void testCreate_whenNameAlreadyExists_shouldThrowDuplicatedException() {
            CategoryPostVm postVm = buildPostVm(CATEGORY_NAME, null);
            when(categoryRepository.findExistedName(CATEGORY_NAME, null))
                .thenReturn(buildCategory(99L, CATEGORY_NAME));

            assertThatThrownBy(() -> categoryService.create(postVm))
                .isInstanceOf(DuplicatedException.class);
        }

        @Test
        void testCreate_whenParentIdProvidedAndParentNotFound_shouldThrowBadRequestException() {
            CategoryPostVm postVm = buildPostVm(CATEGORY_NAME, PARENT_ID);
            when(categoryRepository.findExistedName(CATEGORY_NAME, null)).thenReturn(null);
            when(categoryRepository.findById(PARENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.create(postVm))
                .isInstanceOf(BadRequestException.class);
        }

        @Test
        void testCreate_whenParentExistsAndNameIsUnique_shouldSaveWithParent() {
            CategoryPostVm postVm = buildPostVm(CATEGORY_NAME, PARENT_ID);
            Category parent = buildCategory(PARENT_ID, "Parent");
            Category saved = buildCategory(CATEGORY_ID, CATEGORY_NAME);
            saved.setParent(parent);
            when(categoryRepository.findExistedName(CATEGORY_NAME, null)).thenReturn(null);
            when(categoryRepository.findById(PARENT_ID)).thenReturn(Optional.of(parent));
            when(categoryRepository.save(any(Category.class))).thenReturn(saved);

            Category result = categoryService.create(postVm);

            assertThat(result.getParent()).isNotNull();
            assertThat(result.getParent().getId()).isEqualTo(PARENT_ID);
        }
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Nested
    class UpdateCategoryTest {

        @Test
        void testUpdate_whenCategoryExistsAndNoParent_shouldClearParent() {
            CategoryPostVm postVm = buildPostVm(CATEGORY_NAME, null);
            Category existing = buildCategory(CATEGORY_ID, "OldName");
            when(categoryRepository.findExistedName(CATEGORY_NAME, CATEGORY_ID)).thenReturn(null);
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(existing));

            categoryService.update(postVm, CATEGORY_ID);

            assertThat(existing.getParent()).isNull();
            assertThat(existing.getName()).isEqualTo(CATEGORY_NAME);
        }

        @Test
        void testUpdate_whenCategoryNotFound_shouldThrowNotFoundException() {
            CategoryPostVm postVm = buildPostVm(CATEGORY_NAME, null);
            when(categoryRepository.findExistedName(CATEGORY_NAME, CATEGORY_ID)).thenReturn(null);
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.update(postVm, CATEGORY_ID))
                .isInstanceOf(NotFoundException.class);
        }

        @Test
        void testUpdate_whenNameDuplicated_shouldThrowDuplicatedException() {
            CategoryPostVm postVm = buildPostVm(CATEGORY_NAME, null);
            when(categoryRepository.findExistedName(CATEGORY_NAME, CATEGORY_ID))
                .thenReturn(buildCategory(99L, CATEGORY_NAME));

            assertThatThrownBy(() -> categoryService.update(postVm, CATEGORY_ID))
                .isInstanceOf(DuplicatedException.class);
        }

        @Test
        void testUpdate_whenParentIsSelf_shouldThrowBadRequestException() {
            CategoryPostVm postVm = buildPostVm(CATEGORY_NAME, CATEGORY_ID); // parent = self
            Category existing = buildCategory(CATEGORY_ID, "OldName");
            Category selfAsParent = buildCategory(CATEGORY_ID, "OldName"); // same id
            when(categoryRepository.findExistedName(CATEGORY_NAME, CATEGORY_ID)).thenReturn(null);
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(existing));
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(selfAsParent));

            assertThatThrownBy(() -> categoryService.update(postVm, CATEGORY_ID))
                .isInstanceOf(BadRequestException.class);
        }
    }

    // ── getCategoryById ───────────────────────────────────────────────────────

    @Nested
    class GetCategoryByIdTest {

        @Test
        void testGetCategoryById_whenCategoryExistsWithImage_shouldReturnDetailVm() {
            Category category = buildCategory(CATEGORY_ID, CATEGORY_NAME);
            category.setImageId(1L);
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
            when(mediaService.getMedia(1L)).thenReturn(mediaVm());

            CategoryGetDetailVm result = categoryService.getCategoryById(CATEGORY_ID);

            assertThat(result.name()).isEqualTo(CATEGORY_NAME);
            assertThat(result.categoryImage()).isNotNull();
            assertThat(result.categoryImage().url()).isEqualTo(MEDIA_URL);
        }

        @Test
        void testGetCategoryById_whenCategoryHasNoImage_shouldReturnNullImage() {
            Category category = buildCategory(CATEGORY_ID, CATEGORY_NAME);
            category.setImageId(null);
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));

            CategoryGetDetailVm result = categoryService.getCategoryById(CATEGORY_ID);

            assertThat(result.categoryImage()).isNull();
        }

        @Test
        void testGetCategoryById_whenCategoryHasParent_shouldReturnParentId() {
            Category parent = buildCategory(PARENT_ID, "Parent");
            Category category = buildCategory(CATEGORY_ID, CATEGORY_NAME);
            category.setParent(parent);
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));

            CategoryGetDetailVm result = categoryService.getCategoryById(CATEGORY_ID);

            assertThat(result.parentId()).isEqualTo(PARENT_ID);
        }

        @Test
        void testGetCategoryById_whenCategoryNotFound_shouldThrowNotFoundException() {
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.getCategoryById(CATEGORY_ID))
                .isInstanceOf(NotFoundException.class);
        }
    }

    // ── getCategories ─────────────────────────────────────────────────────────

    @Nested
    class GetCategoriesTest {

        @Test
        void testGetCategories_whenMatchingCategories_shouldReturnList() {
            Category category = buildCategory(CATEGORY_ID, CATEGORY_NAME);
            category.setImageId(1L);
            when(categoryRepository.findByNameContainingIgnoreCase("elec")).thenReturn(List.of(category));
            when(mediaService.getMedia(anyLong())).thenReturn(mediaVm());

            List<CategoryGetVm> result = categoryService.getCategories("elec");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).name()).isEqualTo(CATEGORY_NAME);
        }

        @Test
        void testGetCategories_whenNoMatch_shouldReturnEmptyList() {
            when(categoryRepository.findByNameContainingIgnoreCase(anyString()))
                .thenReturn(Collections.emptyList());

            List<CategoryGetVm> result = categoryService.getCategories("xyz");

            assertThat(result).isEmpty();
        }

        @Test
        void testGetCategories_whenCategoryHasNoImage_shouldReturnNullImage() {
            Category category = buildCategory(CATEGORY_ID, CATEGORY_NAME);
            // imageId is null by default
            when(categoryRepository.findByNameContainingIgnoreCase(anyString()))
                .thenReturn(List.of(category));

            List<CategoryGetVm> result = categoryService.getCategories("elec");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).categoryImage()).isNull();
        }
    }

    // ── getCategoryByIds / getTopNthCategories ────────────────────────────────

    @Nested
    class GetCategoryByIdsTest {

        @Test
        void testGetCategoryByIds_whenIdsMatch_shouldReturnVmList() {
            Category category = buildCategory(CATEGORY_ID, CATEGORY_NAME);
            when(categoryRepository.findAllById(List.of(CATEGORY_ID))).thenReturn(List.of(category));

            List<CategoryGetVm> result = categoryService.getCategoryByIds(List.of(CATEGORY_ID));

            assertThat(result).hasSize(1);
        }

        @Test
        void testGetTopNthCategories_whenLimitApplied_shouldReturnLimitedList() {
            when(categoryRepository.findCategoriesOrderedByProductCount(any(Pageable.class)))
                .thenReturn(List.of("Electronics", "Fashion"));

            List<String> result = categoryService.getTopNthCategories(2);

            assertThat(result).containsExactly("Electronics", "Fashion");
        }
    }
}