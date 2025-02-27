package com.starling.roundup.model.request;

import com.starling.roundup.model.common.Amount;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SavingsGoalRequest {

    private String name;
    private String currency;
    private Amount target;
    private String base64EncodedPhoto;

}