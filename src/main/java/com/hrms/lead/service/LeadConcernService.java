package com.hrms.lead.service;

import com.hrms.core.entity.User;
import com.hrms.core.repository.UserRepository;
import com.hrms.employee.core.entity.Concern;
import com.hrms.employee.core.enums.ConcernStatus;
import com.hrms.employee.core.repository.ConcernRepository;
import com.hrms.employee.payload.request.RaiseConcernRequest; // Reusing
import com.hrms.employee.payload.response.ConcernResponseDTO; // Reusing
import com.hrms.security.service.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LeadConcernService {

    @Autowired
    private ConcernRepository concernRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public ConcernResponseDTO raiseLeadConcern(RaiseConcernRequest request, UserDetailsImpl leadUserDetails) {
        User leadUser = userRepository.findById(leadUserDetails.getId())
                .orElseThrow(() -> new UsernameNotFoundException("Lead user not found: " + leadUserDetails.getUsername()));

        Concern concern = new Concern();
        concern.setRaisedBy(leadUser); // The lead is the one raising the concern
        concern.setConcernText(request.getConcernText());

        String categoryWithHint = request.getCategory() != null ? request.getCategory() : "";
        if (request.getTargetRole() != null && !request.getTargetRole().isBlank()) {
            categoryWithHint = categoryWithHint.isEmpty() ? "Target Hint: " + request.getTargetRole()
                                                          : categoryWithHint + " (Target Hint: " + request.getTargetRole() + ")";
        }
        concern.setCategory(categoryWithHint); // Storing target role hint within category

        concern.setStatus(ConcernStatus.OPEN);
        // raisedAgainstEmployee, raisedAgainstLead, raisedAgainstManager are null for now
        // as the DTO doesn't specify individuals.

        Concern savedConcern = concernRepository.save(concern);
        return mapToConcernResponseDTO(savedConcern);
    }

    // This mapper is identical to the one in employee.service.ConcernService
    // Consider moving to a common utility or mapper class.
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
