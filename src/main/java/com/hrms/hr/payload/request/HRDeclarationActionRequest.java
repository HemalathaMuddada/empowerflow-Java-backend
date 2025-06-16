package com.hrms.hr.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HRDeclarationActionRequest {

    @NotBlank(message = "Action cannot be blank.")
    @Pattern(regexp = "APPROVE|REJECT", message = "Action must be either APPROVE or REJECT.")
    private String action;

    @Size(max = 1000, message = "HR comments must be less than 1000 characters.")
    private String hrComments; // Nullable
}
