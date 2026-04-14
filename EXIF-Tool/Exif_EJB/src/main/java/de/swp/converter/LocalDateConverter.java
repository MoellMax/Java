package de.swp.converter;

import jakarta.persistence.*;
import java.time.*;
import java.time.format.*;

@Converter(autoApply = false)
public class LocalDateConverter implements AttributeConverter<LocalDate, String> {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yy");

    @Override
    public String convertToDatabaseColumn(LocalDate date) {
        return (date == null) ? null : date.format(FORMATTER);
    }

    @Override
    public LocalDate convertToEntityAttribute(String dbData) {
        return (dbData == null) ? null : LocalDate.parse(dbData, FORMATTER);
    }
}
 
