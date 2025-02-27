package com.starling.roundup.model.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class StarlingAPISavingGoalsResponse {

    private List<SavingsGoal> savingsGoalList;
}
