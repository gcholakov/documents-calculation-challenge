package com.challenge.docscalc.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.challenge.docscalc.api.DefaultApi;
import com.challenge.docscalc.model.CalculateResponse;
import com.challenge.docscalc.service.CalcInvoiceService;

@RestController
public class CalcInvoiceController implements DefaultApi {

    private final CalcInvoiceService calcInvoiceService;

    @Autowired
    public CalcInvoiceController(CalcInvoiceService calcInvoiceService) {

        this.calcInvoiceService = calcInvoiceService;
    }

    @Override
    public ResponseEntity<CalculateResponse> sumInvoices(MultipartFile file, List<String> exchangeRates,
            String outputCurrency, String customerVat) {

        try {
            return ResponseEntity.ok()
                    .body(calcInvoiceService.calculateInvoices(file, exchangeRates, outputCurrency, customerVat));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
