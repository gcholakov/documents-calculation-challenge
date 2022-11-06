package com.challenge.docscalc;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.challenge.docscalc.service.CalcInvoiceServiceImpl;

@WebMvcTest
class CalcInvoiceControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockBean
    private CalcInvoiceServiceImpl calcInvoiceService;

    @Test
    public void sumInvoices_whenValidInput_returnStatusOK() throws Exception {

        Path path = Paths.get("src/test/resources/test_data.csv");
        byte[] data = Files.readAllBytes(path);

        MockMultipartFile notEmptyFile = new MockMultipartFile("file", "test_data.csv",
                MediaType.TEXT_PLAIN_VALUE, data);

        when(calcInvoiceService.calculateInvoices(any(), any(), any(), any())).thenCallRealMethod();
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        MvcResult mvcResult = mockMvc.perform(multipart("/api/v1/sumInvoices")
                        .file(notEmptyFile)
                        .param("exchangeRates", "EUR:1,USD:0.987,GBP:0.878")
                        .param("outputCurrency", "USD")
                        .param("customerVat", ""))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        String jsonResult = mvcResult.getResponse().getContentAsString();
        assertTrue(jsonResult.contains("Vendor 1"));
        assertTrue(jsonResult.contains("Vendor 2"));
        assertTrue(jsonResult.contains("Vendor 3"));
    }

    @Test
    public void sumInvoices_whenInvalidInput_returnError() throws Exception {

        Path path = Paths.get("src/test/resources/test_data.csv");
        byte[] data = Files.readAllBytes(path);

        MockMultipartFile notEmptyFile = new MockMultipartFile("file", "test_data.csv",
                MediaType.TEXT_PLAIN_VALUE, data);

        when(calcInvoiceService.calculateInvoices(any(), any(), any(), any())).thenCallRealMethod();
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        MvcResult mvcResult = mockMvc.perform(multipart("/api/v1/sumInvoices")
                        .file(notEmptyFile)
                        .param("exchangeRates", "EUR:1,USD:0.987,GBP:0.878")
                        .param("outputCurrency", "US")
                        .param("customerVat", ""))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        String jsonResult = mvcResult.getResponse().getContentAsString();
        assertTrue(jsonResult.contains("timestamp"));
        assertTrue(jsonResult.contains("message"));
    }
}