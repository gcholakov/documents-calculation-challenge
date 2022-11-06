package com.challenge.docscalc.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.challenge.docscalc.exception.ValidationException;
import com.challenge.docscalc.model.CalculateResponse;
import com.challenge.docscalc.model.Customer;
import com.challenge.docscalc.model.Invoice;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;

/**
 * {@inheritDoc}
 */
@Service
public class CalcInvoiceServiceImpl implements CalcInvoiceService {

    /**
     * {@inheritDoc}
     */
    @Override
    public CalculateResponse calculateInvoices(MultipartFile file, List<String> exchangeRates, String outputCurrency,
            String customerVat) throws Exception {

        validateInputParams(file, exchangeRates, outputCurrency);

        Map<String, BigDecimal> exchangeRatesMap = extractExchangeRates(exchangeRates);

        List<Customer> customersBalancesList = calculateBalancesFromInvoices(file, outputCurrency,
                exchangeRatesMap, customerVat);

        CalculateResponse calculateResponse = new CalculateResponse();
        calculateResponse.setCustomers(customersBalancesList);
        calculateResponse.setCurrency(outputCurrency);
        return calculateResponse;
    }

    /**
     * Validation of input parameters.
     * @param file file with data
     * @param exchangeRates exchange rates
     * @param outputCurrency targeted currency for final balances
     * @throws ValidationException if validation fails
     */
    private void validateInputParams(MultipartFile file, List<String> exchangeRates, String outputCurrency)
            throws ValidationException {

        if (CollectionUtils.isEmpty(exchangeRates)) {
            throw new ValidationException("Exchange rates not provided!");
        }

        if (file.isEmpty()) {
            throw new ValidationException("File must not be empty!");
        }

        if (StringUtils.isBlank(outputCurrency)) {
            throw new ValidationException("Output currency must be provided!");
        }

        Map<String, BigDecimal> exchangeRatesMap = extractExchangeRates(exchangeRates);

        Optional<Entry<String, BigDecimal>> defaultCcyEntry = exchangeRatesMap.entrySet().stream()
                .filter(e -> e.getValue().equals(BigDecimal.ONE)).findFirst();

        if (!defaultCcyEntry.isPresent()) {
            throw new ValidationException("No default currency provided!");
        }

        if (!exchangeRatesMap.containsKey(outputCurrency)) {
            throw new ValidationException("Output currency not found in exchange rates!");
        }
    }

    /**
     * For extracting exchange rates from provided parameter.
     * @param exchangeRates input parameter with rates
     * @return a Map with currency and exchange rate
     */
    private Map<String, BigDecimal> extractExchangeRates(List<String> exchangeRates) {

        return exchangeRates.stream()
                .collect(Collectors.toMap(r -> r.substring(0, 3), r -> new BigDecimal(r.substring(4))));
    }

    /**
     * Calculates balances from invoices.
     * @param file file with data
     * @param outputCcy targeted currency
     * @param exchangeRatesMap exchange rates
     * @param customerVat customer VAT for filtering, if present
     * @return list with customers and balances
     * @throws Exception if data file was not parsed
     */
    private List<Customer> calculateBalancesFromInvoices(MultipartFile file, String outputCcy,
            Map<String, BigDecimal> exchangeRatesMap, String customerVat) throws Exception {

        List<Invoice> invoices;

        try (Reader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {

            //noinspection unchecked,rawtypes
            CsvToBean<Invoice> csvToBean = new CsvToBeanBuilder(reader)
                    .withType(Invoice.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build();

            invoices = csvToBean.parse();
        } catch (Exception e) {
            throw new ValidationException("Error parsing the file: " + e.getMessage());
        }

        Map<String, BigDecimal> balancesMap = new HashMap<>();

        if (!StringUtils.isBlank(customerVat)) {
            invoices = invoices.stream()
                    .filter(i -> i.getVatNumber().equals(customerVat))
                    .collect(Collectors.toList());
        }

        invoices.forEach(i -> {
            BigDecimal invoiceTotalOutputCcy = convertAndSign(i, outputCcy, exchangeRatesMap);
            balancesMap.compute(i.getCustomer(),
                    (k, v) -> v == null ? invoiceTotalOutputCcy : v.add(invoiceTotalOutputCcy));
        });

        return balancesMap.entrySet().stream()
                .map(e -> new Customer().name(e.getKey()).balance(e.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * Used for conversion between currencies and eventually inverting amount to negative if credit note found.
     * @param invoice a line from input file
     * @param outputCcy targeted currency
     * @param exchangeRatesMap rates
     * @return the value in targeted currency, negative if the invoice is credit note
     */
    private BigDecimal convertAndSign(Invoice invoice, String outputCcy, Map<String, BigDecimal> exchangeRatesMap) {

        BigDecimal totalDefaultCcy;
        BigDecimal totalOutputCcy;

        if (invoice.getCurrency().equals(outputCcy)) {
            totalOutputCcy = invoice.getTotal();
        } else {
            BigDecimal rateInvoiceCcy = exchangeRatesMap.get(invoice.getCurrency());
            totalDefaultCcy = invoice.getTotal().divide(rateInvoiceCcy, 4, RoundingMode.HALF_UP);

            BigDecimal rateOutputCcy = exchangeRatesMap.get(outputCcy);
            totalOutputCcy = totalDefaultCcy.multiply(rateOutputCcy);
        }

        if (invoice.getType() == 2) {
            return totalOutputCcy.negate();
        }

        return totalOutputCcy;
    }
}
