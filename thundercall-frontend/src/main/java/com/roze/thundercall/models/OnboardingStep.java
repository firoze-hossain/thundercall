package com.roze.thundercall.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OnboardingStep {
    private String id;
    private String title;
    private String description;
    private int stepNumber;
    private boolean completed;
}
