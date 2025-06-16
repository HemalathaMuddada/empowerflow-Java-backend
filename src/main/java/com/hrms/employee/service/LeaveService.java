package com.hrms.employee.service;

import com.hrms.core.entity.User;
import com.hrms.core.repository.UserRepository;
import com.hrms.employee.core.entity.LeaveRequest;
import com.hrms.employee.core.enums.LeaveStatus;
import com.hrms.employee.core.enums.LeaveType;
import com.hrms.employee.core.repository.LeaveRequestRepository;
import com.hrms.employee.payload.request.LeaveApplicationRequest;
import com.hrms.employee.payload.response.EmployeeLeaveSummaryResponse;
import com.hrms.employee.payload.response.LeaveBalanceDTO;
import com.hrms.employee.payload.response.LeaveHistoryResponse;
import com.hrms.employee.payload.response.LeaveRequestDetailsDTO;
import com.hrms.security.service.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.EnumMap;


@Service
public class LeaveService {

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private UserRepository userRepository;

    // Placeholder for default annual entitlements
    private static final Map<LeaveType, Double> ANNUAL_ENTITLEMENTS = new EnumMap<>(LeaveType.class);

    static {
        ANNUAL_ENTITLEMENTS.put(LeaveType.SICK, 10.0);
        ANNUAL_ENTITLEMENTS.put(LeaveType.PRIVILEGE, 15.0);
        ANNUAL_ENTITLEMENTS.put(LeaveType.PERSONAL, 5.0);
        ANNUAL_ENTITLEMENTS.put(LeaveType.MATERNITY, 180.0); // Example, can be complex
        ANNUAL_ENTITLEMENTS.put(LeaveType.BEREAVEMENT, 5.0);
    }

    @Transactional(readOnly = true)
    public EmployeeLeaveSummaryResponse getLeaveBalances(UserDetailsImpl currentUser) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + currentUser.getUsername()));

        List<LeaveRequest> approvedLeaves = leaveRequestRepository.findByEmployeeAndStatus(user, LeaveStatus.APPROVED);

        List<LeaveBalanceDTO> balances = new ArrayList<>();
        for (LeaveType type : LeaveType.values()) {
            double totalEntitled = ANNUAL_ENTITLEMENTS.getOrDefault(type, 0.0);
            double availed = approvedLeaves.stream()
                    .filter(lr -> lr.getLeaveType() == type)
                    .mapToDouble(lr -> ChronoUnit.DAYS.between(lr.getStartDate(), lr.getEndDate()) + 1) // Inclusive of end date
                    .sum();
            double balance = totalEntitled - availed;
            balances.add(new LeaveBalanceDTO(type.name(), totalEntitled, availed, balance));
        }
        return new EmployeeLeaveSummaryResponse(balances);
    }

    @Transactional
    public LeaveRequestDetailsDTO applyForLeave(LeaveApplicationRequest request, UserDetailsImpl currentUser) {
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new IllegalArgumentException("Leave start date cannot be after end date.");
        }

        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + currentUser.getUsername()));

        LeaveType leaveType;
        try {
            leaveType = LeaveType.valueOf(request.getLeaveType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid leave type: " + request.getLeaveType());
        }

        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setEmployee(user);
        leaveRequest.setLeaveType(leaveType);
        leaveRequest.setStartDate(request.getStartDate());
        leaveRequest.setEndDate(request.getEndDate());
        leaveRequest.setReason(request.getReason());
        leaveRequest.setStatus(LeaveStatus.PENDING);
        // createdAt and updatedAt will be set by AuditingEntityListener

        LeaveRequest savedRequest = leaveRequestRepository.save(leaveRequest);
        return mapToLeaveRequestDetailsDTO(savedRequest);
    }

    @Transactional(readOnly = true)
    public LeaveHistoryResponse getLeaveHistory(UserDetailsImpl currentUser) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + currentUser.getUsername()));

        List<LeaveRequest> leaveRequests = leaveRequestRepository.findByEmployeeOrderByCreatedAtDesc(user);
        List<LeaveRequestDetailsDTO> dtoList = leaveRequests.stream()
                .map(this::mapToLeaveRequestDetailsDTO)
                .collect(Collectors.toList());
        return new LeaveHistoryResponse(dtoList);
    }

    private LeaveRequestDetailsDTO mapToLeaveRequestDetailsDTO(LeaveRequest leaveRequest) {
        return new LeaveRequestDetailsDTO(
                leaveRequest.getId(),
                leaveRequest.getLeaveType().name(),
                leaveRequest.getStartDate(),
                leaveRequest.getEndDate(),
                leaveRequest.getReason(),
                leaveRequest.getStatus().name(),
                leaveRequest.getManagerComment(),
                leaveRequest.getCreatedAt()
        );
    }
}
