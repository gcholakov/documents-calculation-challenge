package com.challenge.docscalc.model;

import java.math.BigDecimal;

import com.opencsv.bean.CsvBindByName;

import lombok.Getter;

/**
 * Used to represent a line with invoice from data file.
 */
@Getter
public class Invoice {

    @CsvBindByName
    private String customer;

    @CsvBindByName(column = "Vat number")
    private String vatNumber;

    @CsvBindByName(column = "Document number")
    private String docNumber;

    @CsvBindByName
    private int type;

    @CsvBindByName(column = "Parent document")
    private String parentDoc;

    @CsvBindByName
    private String currency;

    @CsvBindByName
    private BigDecimal total;
}
