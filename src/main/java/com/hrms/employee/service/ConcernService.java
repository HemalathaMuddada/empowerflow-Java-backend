package com.hrms.employee.service;

import com.hrms.core.entity.User;
import com.hrms.core.repository.UserRepository;
import com.hrms.employee.core.entity.Concern;
import com.hrms.employee.core.enums.ConcernStatus;
import com.hrms.employee.core.repository.ConcernRepository;
import com.hrms.employee.payload.request.RaiseConcernRequest;
import com.hrms.employee.payload.response.ConcernResponseDTO;
import com.hrms.security.service.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConcernService {

    @Autowired
    private ConcernRepository concernRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public ConcernResponseDTO raiseConcern(RaiseConcernRequest request, UserDetailsImpl currentUserDetails) {
        User raisedByUser = userRepository.findById(currentUserDetails.getId())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + currentUserDetails.getUsername()));

        Concern concern = new Concern();
        concern.setRaisedBy(raisedByUser);
        concern.setConcernText(request.getConcernText());

        String categoryWithHint = request.getCategory() != null ? request.getCategory() : "";
        if (request.getTargetRole() != null && !request.getTargetRole().isBlank()) {
            categoryWithHint = categoryWithHint.isEmpty() ? "Target: " + request.getTargetRole()
                                                          : categoryWithHint + " (Target: " + request.getTargetRole() + ")";
        }
        concern.setCategory(categoryWithHint); // Storing target role hint within category for now

        concern.setStatus(ConcernStatus.OPEN);
        // raisedAgainstEmployee, raisedAgainstLead, raisedAgainstManager are left null as per current DTO

        Concern savedConcern = concernRepository.save(concern);
        return mapToConcernResponseDTO(savedConcern);
    }

    private ConcernResponseDTO mapToConcernResponseDTO(Concern concern) {
        return new ConcernResponseDTO(
                concern.getId(),
                concern.getRaisedBy().getId(),
                concern.getConcernText(),
                concern.getCategory(),
                concern.getStatus().name(),
                concern.getCreatedAt()
        );
    }
}
