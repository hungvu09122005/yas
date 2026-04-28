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
import com.yas.tax.model.TaxRate;
import com.yas.tax.service.TaxRateService;
import com.yas.tax.viewmodel.taxrate.TaxRateListGetVm;
import com.yas.tax.viewmodel.taxrate.TaxRatePostVm;
import com.yas.tax.viewmodel.taxrate.TaxRateVm;
import java.util.List;
import org.instancio.Instancio;
import static org.instancio.Select.field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(value = TaxRateController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class})
public class TaxRateControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    TaxRateService taxRateService;

    TaxRate taxRate;
    TaxRateVm taxRateVm;

    @BeforeEach
    void setUp() {
        TaxClass taxClass = Instancio.create(TaxClass.class);
        taxRate = Instancio.of(TaxRate.class)
                .set(field("taxClass"), taxClass)
                .create();
        taxRateVm = TaxRateVm.fromModel(taxRate);
    }

    @Test
    void getPageableTaxRates_shouldReturn200() throws Exception {
        TaxRateListGetVm listVm = new TaxRateListGetVm(List.of(), 0, 10, 0, 0, true);
        when(taxRateService.getPageableTaxRates(0, 10)).thenReturn(listVm);

        mockMvc.perform(get("/backoffice/tax-rates/paging")
                        .param("pageNo", "0")
                        .param("pageSize", "10"))
                .andExpect(status().isOk());
    }

    @Test
    void getTaxRateById_shouldReturn200() throws Exception {
        when(taxRateService.findById(taxRate.getId())).thenReturn(taxRateVm);

        mockMvc.perform(get("/backoffice/tax-rates/{id}", taxRate.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taxRateVm.id()));
    }

    @Test
    void createTaxRate_shouldReturn201() throws Exception {
        TaxRatePostVm postVm = new TaxRatePostVm(10.0, "12345", 1L, 1L, 1L);
        when(taxRateService.createTaxRate(any())).thenReturn(taxRate);

        mockMvc.perform(post("/backoffice/tax-rates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(postVm)))
                .andExpect(status().isCreated());
    }

    @Test
    void updateTaxRate_shouldReturn204() throws Exception {
        TaxRatePostVm postVm = new TaxRatePostVm(15.0, "54321", 1L, 1L, 1L);
        doNothing().when(taxRateService).updateTaxRate(any(), anyLong());

        mockMvc.perform(put("/backoffice/tax-rates/{id}", taxRate.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(postVm)))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteTaxRate_shouldReturn204() throws Exception {
        doNothing().when(taxRateService).delete(anyLong());

        mockMvc.perform(delete("/backoffice/tax-rates/{id}", taxRate.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    void getTaxPercentByAddress_shouldReturn200() throws Exception {
        when(taxRateService.getTaxPercent(1L, 1L, 1L, "12345")).thenReturn(10.0);

        mockMvc.perform(get("/backoffice/tax-rates/tax-percent")
                        .param("taxClassId", "1")
                        .param("countryId", "1")
                        .param("stateOrProvinceId", "1")
                        .param("zipCode", "12345"))
                .andExpect(status().isOk());
    }

    @Test
    void getBatchTaxPercentsByAddress_shouldReturn200() throws Exception {
        when(taxRateService.getBulkTaxRate(any(), any(), any(), any()))
                .thenReturn(List.of(taxRateVm));

        mockMvc.perform(get("/backoffice/tax-rates/location-based-batch")
                        .param("taxClassIds", "1", "2")
                        .param("countryId", "1"))
                .andExpect(status().isOk());
    }
}
