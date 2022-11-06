package com.challenge.docscalc.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.challenge.docscalc.model.CalculateResponse;

/**
 * Service for calculating invoices to form balances.
 */
public interface CalcInvoiceService {

    /**
     * Used for calculation of invoices.
     * @param file file with data
     * @param exchangeRates exchange rates
     * @param outputCurrency targeted currency
     * @param customerVat customer VAT for filtering, if present
     * @return actual response
     * @throws Exception if parsing the file fails
     */
    CalculateResponse calculateInvoices(MultipartFile file, List<String> exchangeRates,
            String outputCurrency, String customerVat) throws Exception;
}
