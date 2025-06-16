package com.hrms.employee.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BirthdayGreetingResponse {
    private boolean isBirthday;
    private String greetingMessage; // Nullable
    private String imageUrl;        // Nullable
    private String audioUrl;        // Nullable
}
