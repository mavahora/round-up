package com.starling.roundup.model.response;

import io.micrometer.common.util.StringUtils;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.util.CollectionUtils.isEmpty;

@Getter
@Setter
public class StarlingErrorResponse {
    private List<StarlingError> errors;
    private boolean success;

    public String getConcatenatedErrorMessages() {
        if (isEmpty(errors)) {
            return "No error message returned from Starling API.";
        }
        return errors.stream()
                .map(StarlingError::getMessage)
                .filter(msg -> !StringUtils.isEmpty(msg))
                .collect(Collectors.joining(" | "));
    }

}