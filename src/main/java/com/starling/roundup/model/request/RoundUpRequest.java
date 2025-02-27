package com.starling.roundup.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

import static java.time.DayOfWeek.MONDAY;

@Getter
@Setter
public class RoundUpRequest {

    @NotNull(message = "accountUid is required.")
    private String accountUid;

    @NotBlank(message = "savingsGoalUid is required.")
    private String savingsGoalUid;

    @NotBlank(message = "weekCommencing date is required.")
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "Date must be in the format yyyy-MM-dd.")
    private String weekCommencing;

    // Week commencing should be Monday and the week has already ended
    public LocalDate isWeekCommencingValid() {
        LocalDate date;
        try {
            date = LocalDate.parse(this.weekCommencing);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date format. Could not parse weekCommencing DATE");
        }

        if (date.getDayOfWeek() != MONDAY) {
            throw new IllegalArgumentException("Week commencing date must be a Monday");
        }

        if (!date.isBefore(LocalDate.now().with(MONDAY))) {
            throw new IllegalArgumentException("weekCommencing date must be a completed week");
        }

        return date;
    }

}