package com.yas.tax.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yas.tax.model.TaxClass;
import com.yas.tax.service.TaxClassService;
import com.yas.tax.viewmodel.taxclass.TaxClassListGetVm;
import com.yas.tax.viewmodel.taxclass.TaxClassPostVm;
import com.yas.tax.viewmodel.taxclass.TaxClassVm;
import java.util.List;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(value = TaxClassController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class})
public class TaxClassControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    TaxClassService taxClassService;

    TaxClass taxClass;
    TaxClassVm taxClassVm;

    @BeforeEach
    void setUp() {
        taxClass = Instancio.create(TaxClass.class);
        taxClassVm = TaxClassVm.fromModel(taxClass);
    }

    @Test
    void listTaxClasses_shouldReturn200WithList() throws Exception {
        when(taxClassService.findAllTaxClasses()).thenReturn(List.of(taxClassVm));

        mockMvc.perform(get("/backoffice/tax-classes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(taxClassVm.id()));
    }

    @Test
    void getPageableTaxClasses_shouldReturn200() throws Exception {
        TaxClassListGetVm listGetVm = new TaxClassListGetVm(
                List.of(taxClassVm), 0, 10, 1, 1, true);
        when(taxClassService.getPageableTaxClasses(0, 10)).thenReturn(listGetVm);

        mockMvc.perform(get("/backoffice/tax-classes/paging")
                        .param("pageNo", "0")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taxClassContent[0].id").value(taxClassVm.id()));
    }

    @Test
    void getTaxClassById_shouldReturn200() throws Exception {
        when(taxClassService.findById(taxClass.getId())).thenReturn(taxClassVm);

        mockMvc.perform(get("/backoffice/tax-classes/{id}", taxClass.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taxClassVm.id()));
    }

    @Test
    void createTaxClass_shouldReturn201() throws Exception {
        TaxClassPostVm postVm = new TaxClassPostVm("id-1", "Standard Tax");
        when(taxClassService.create(any())).thenReturn(taxClass);

        mockMvc.perform(post("/backoffice/tax-classes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(postVm)))
                .andExpect(status().isCreated());
    }

    @Test
    void updateTaxClass_shouldReturn204() throws Exception {
        TaxClassPostVm postVm = new TaxClassPostVm("id-1", "Updated Tax");
        doNothing().when(taxClassService).update(any(), anyLong());

        mockMvc.perform(put("/backoffice/tax-classes/{id}", taxClass.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(postVm)))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteTaxClass_shouldReturn204() throws Exception {
        doNothing().when(taxClassService).delete(anyLong());

        mockMvc.perform(delete("/backoffice/tax-classes/{id}", taxClass.getId()))
                .andExpect(status().isNoContent());
    }
}
