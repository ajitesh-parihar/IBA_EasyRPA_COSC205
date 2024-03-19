package ca.bc.okanagan.ok_ap1_accounting_ie.model.adapters;

import eu.ibagroup.easyrpa.persistence.TypeAdaptor;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class LocalDateAdapter implements TypeAdaptor<LocalDate> {

    private static final DateTimeFormatter PERIOD_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    @Override
    public LocalDate adaptString(Field f, String s) {
        return LocalDate.parse(s, PERIOD_FORMATTER);
    }

    @Override
    public String adaptType(LocalDate localDate) {
        return localDate.format(PERIOD_FORMATTER);
    }
}
