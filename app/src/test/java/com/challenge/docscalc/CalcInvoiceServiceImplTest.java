package com.challenge.docscalc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import com.challenge.docscalc.exception.ValidationException;
import com.challenge.docscalc.model.CalculateResponse;
import com.challenge.docscalc.service.CalcInvoiceServiceImpl;

@ExtendWith(MockitoExtension.class)
class CalcInvoiceServiceImplTest {

    @Mock
    private CalcInvoiceServiceImpl calcInvoiceService;

    @Test
    void calculateInvoices_whenInvalidInput_thenValidationFailed() throws Exception {

        MockMultipartFile emptyFile = new MockMultipartFile("fileName", "data.csv",
                MediaType.TEXT_PLAIN_VALUE, "".getBytes());
        MockMultipartFile notEmptyFile = new MockMultipartFile("fileName", "data.csv",
                MediaType.TEXT_PLAIN_VALUE, "test data".getBytes());
        List<String> exchangeRatesNoDefaultCcy = Arrays.asList("USD:0.987", "GBP:0.878");
        List<String> exchangeRatesDefaultCcy = Arrays.asList("EUR:1", "USD:0.987", "GBP:0.878");
        String outputCurrency = "GBP";

        when(calcInvoiceService.calculateInvoices(any(), any(), any(), any())).thenCallRealMethod();

        Exception exception = assertThrows(ValidationException.class,
                () -> calcInvoiceService.calculateInvoices(null, null, null, null));
        assertEquals("Exchange rates not provided!", exception.getMessage());

        exception = assertThrows(ValidationException.class,
                () -> calcInvoiceService.calculateInvoices(emptyFile, exchangeRatesDefaultCcy, null, null));
        assertEquals("File must not be empty!", exception.getMessage());

        exception = assertThrows(ValidationException.class,
                () -> calcInvoiceService.calculateInvoices(notEmptyFile, exchangeRatesDefaultCcy, "", null));
        assertEquals("Output currency must be provided!", exception.getMessage());

        exception = assertThrows(ValidationException.class,
                () -> calcInvoiceService.calculateInvoices(notEmptyFile, exchangeRatesNoDefaultCcy, outputCurrency,
                        null));
        assertEquals("No default currency provided!", exception.getMessage());

        exception = assertThrows(ValidationException.class,
                () -> calcInvoiceService.calculateInvoices(notEmptyFile, exchangeRatesDefaultCcy, "BGN", null));
        assertEquals("Output currency not found in exchange rates!", exception.getMessage());
    }

    @Test
    void calculateInvoices_whenValidInput_thenSuccessfulResult() throws Exception {

        Path path = Paths.get("src/test/resources/test_data.csv");
        byte[] data = Files.readAllBytes(path);

        MockMultipartFile notEmptyFile = new MockMultipartFile("file", "test_data.csv",
                MediaType.TEXT_PLAIN_VALUE, data);
        List<String> exchangeRates = Arrays.asList("EUR:1", "USD:0.987", "GBP:0.878");
        String outputCurrency = "GBP";

        when(calcInvoiceService.calculateInvoices(any(), any(), any(), any())).thenCallRealMethod();

        CalculateResponse calculateResponse = calcInvoiceService.calculateInvoices(notEmptyFile, exchangeRates,
                outputCurrency, null);
        assertEquals("GBP", calculateResponse.getCurrency());
        assertEquals(3, calculateResponse.getCustomers().size());

        calculateResponse = calcInvoiceService.calculateInvoices(notEmptyFile, exchangeRates,
                outputCurrency, "123456789");
        assertEquals(1, calculateResponse.getCustomers().size());
        assertEquals("Vendor 1", calculateResponse.getCustomers().get(0).getName());
        assertEquals(1722, calculateResponse.getCustomers().get(0).getBalance().intValue());
    }
}