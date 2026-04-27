package com.yas.product.viewmodel.product;

import com.yas.product.model.Brand;
import com.yas.product.model.Product;
import org.junit.jupiter.api.Test;
import java.time.ZonedDateTime;
import static org.assertj.core.api.Assertions.assertThat;

class ProductCheckoutListVmTest {

    @Test
    void testFromModel_whenParentAndBrandAreNull_ShouldMapCorrectly() {
        Product product = new Product();
        product.setId(1L);
        product.setName("Product A");
        product.setParent(null);
        product.setBrand(null);
        product.setCreatedOn(ZonedDateTime.now());

        ProductCheckoutListVm vm = ProductCheckoutListVm.fromModel(product);

        assertThat(vm.id()).isEqualTo(1L);
        assertThat(vm.name()).isEqualTo("Product A");
        assertThat(vm.parentId()).isNull();
        assertThat(vm.brandId()).isNull();
    }

    @Test
    void testFromModel_whenParentAndBrandAreNotNull_ShouldMapCorrectly() {
        Product parent = new Product();
        parent.setId(10L);

        Brand brand = new Brand();
        brand.setId(20L);

        Product product = new Product();
        product.setId(2L);
        product.setName("Product B");
        product.setParent(parent);
        product.setBrand(brand);

        ProductCheckoutListVm vm = ProductCheckoutListVm.fromModel(product);

        assertThat(vm.id()).isEqualTo(2L);
        assertThat(vm.parentId()).isEqualTo(10L);
        assertThat(vm.brandId()).isEqualTo(20L);
    }
}
