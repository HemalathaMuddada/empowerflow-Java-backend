package com.hrms.employee.payload.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeFinalCommentsRequest {

    @Size(max = 20000, message = "Employee comments text is too long.") // Assuming TEXT column
    private String employeeComments; // Nullable - employee might just acknowledge
}
