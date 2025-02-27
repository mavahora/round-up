package com.starling.roundup.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.starling.roundup.model.common.Amount;
import lombok.Getter;
import lombok.Setter;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Getter
@Setter
public class SavingsGoal {

    private String savingsGoalUid;
    private String name;
    @JsonInclude(NON_NULL)
    private Amount target;
    @JsonInclude(NON_NULL)
    private String state;

    public SavingsGoal(String savingsGoalUid, String name) {
        this.savingsGoalUid = savingsGoalUid;
        this.name = name;
    }
}
