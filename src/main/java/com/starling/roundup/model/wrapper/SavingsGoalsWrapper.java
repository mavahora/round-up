package com.starling.roundup.model.wrapper;

import com.starling.roundup.model.response.SavingsGoal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class SavingsGoalsWrapper {
    private final List<SavingsGoal> savingsGoals;
    private final boolean newGoalCreated;
}