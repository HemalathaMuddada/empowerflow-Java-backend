package com.hrms.hr.payload.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReassignReporteesRequest {

    @NotNull(message = "Offboarded Manager ID cannot be null.")
    private Long offboardedManagerId;

    @NotNull(message = "New Manager ID cannot be null.")
    private Long newManagerId;
}
