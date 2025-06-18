package com.hrms.lead.service;

import com.hrms.core.entity.User;
import com.hrms.core.repository.UserRepository;
import com.hrms.employee.core.entity.Hike;
import com.hrms.employee.core.repository.HikeRepository;
import com.hrms.lead.payload.response.TeamHikeSummaryResponse;
import com.hrms.lead.payload.response.TeamMemberHikeDTO;
import com.hrms.security.service.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LeadHikeService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HikeRepository hikeRepository;

    @Transactional(readOnly = true)
    public TeamHikeSummaryResponse getTeamHikeInformation(UserDetailsImpl leadUserDetails) {
        Long leadUserId = leadUserDetails.getId();
        List<User> reportees = userRepository.findByManagerId(leadUserId);

        if (reportees.isEmpty()) {
            return new TeamHikeSummaryResponse(Collections.emptyList());
        }

        List<Long> reporteeIds = reportees.stream().map(User::getId).collect(Collectors.toList());
        List<Hike> teamHikes = hikeRepository.findByEmployeeIdInOrderByEmployee_IdAscEffectiveDateDesc(reporteeIds);

        List<TeamMemberHikeDTO> dtoList = teamHikes.stream()
                .map(this::mapToTeamMemberHikeDTO)
                .collect(Collectors.toList());

        return new TeamHikeSummaryResponse(dtoList);
    }

    private TeamMemberHikeDTO mapToTeamMemberHikeDTO(Hike hike) {
        User employee = hike.getEmployee();
        String employeeName = (employee.getFirstName() != null ? employee.getFirstName() : "")
                             + " " + (employee.getLastName() != null ? employee.getLastName() : "");

        return new TeamMemberHikeDTO(
                employee.getId(),
                employeeName.trim(),
                hike.getId(),
                hike.getHikePercentage(),
                hike.getHikeAmount(),
                hike.getOldSalary(),
                hike.getNewSalary(),
                hike.getEffectiveDate(),
                hike.getPromotionTitle(),
                hike.getProcessedAt(),
                hike.getComments()
        );
    }
}
