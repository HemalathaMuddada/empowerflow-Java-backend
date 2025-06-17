package com.hrms.hr.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HolidayManagementRequest {

    @NotBlank(message = "Holiday name cannot be blank")
    @Size(max = 255, message = "Holiday name must be less than 255 characters")
    private String name;

    @NotNull(message = "Date cannot be null")
    private LocalDate date;

    @Size(max = 1000, message = "Description must be less than 1000 characters")
    private String description; // Nullable

    @NotNull(message = "isGlobal flag must be provided")
    private Boolean isGlobal = false; // Default to false
}
