package com.starling.roundup.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoundUpStatusResponse {

    public RoundUpStatusResponse(StatusResponse status) {
        this.status = status;
    }

    private StatusResponse status;
    @JsonInclude(NON_NULL)
    private String roundUpAmount;
}
