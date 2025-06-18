package com.hrms.hr.payload.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PublishHikesRequest {

    @NotEmpty(message = "List of hike record IDs cannot be empty.")
    private List<@NotNull Long> hikeRecordIds;
}
