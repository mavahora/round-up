package com.starling.roundup.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

import static jakarta.persistence.EnumType.STRING;

@Entity
@Table(name = "round_up_requests", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"accountId", "weekCommencing"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoundUpRequest {

    @Id
    private String requestId;

    @Column(nullable = false)
    private String accountId;

    @Column(nullable = false)
    private LocalDate weekCommencing;

    @Enumerated(STRING)
    @Column(nullable = false)
    private Status status;

    @Column(nullable = false)
    private long roundUpAmount;


}


