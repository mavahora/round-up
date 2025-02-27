package com.starling.roundup.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Getter
@Setter
@NoArgsConstructor
public class AccountDetailsResponse {

    private String accountUid;
    private String accountName;
    private List<SavingsGoal> savingsGoalList;
    @JsonInclude(NON_NULL)
    private String message;
}