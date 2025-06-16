package com.hrms.employee.service;

import com.hrms.core.repository.UserRepository; // Not strictly needed if only ID is used from UserDetailsImpl
import com.hrms.employee.core.entity.Hike;
import com.hrms.employee.core.repository.HikeRepository;
import com.hrms.employee.payload.response.HikeDetailDTO;
import com.hrms.employee.payload.response.HikeHistoryResponse;
import com.hrms.security.service.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.security.core.userdetails.UsernameNotFoundException; // Might not be needed
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class HikeService {

    @Autowired
    private HikeRepository hikeRepository;

    // UserRepository might not be needed if we directly use currentUserDetails.getId()
    // @Autowired
    // private UserRepository userRepository;

    @Transactional(readOnly = true)
    public HikeHistoryResponse getMyHikeHistory(UserDetailsImpl currentUserDetails) {
        // User user = userRepository.findById(currentUserDetails.getId())
        //        .orElseThrow(() -> new UsernameNotFoundException("User not found: " + currentUserDetails.getUsername()));
        // We can directly use currentUserDetails.getId() for the repository query

        List<Hike> hikes = hikeRepository.findByEmployeeIdOrderByEffectiveDateDesc(currentUserDetails.getId());

        List<HikeDetailDTO> dtoList = hikes.stream()
                .map(this::mapToHikeDetailDTO)
                .collect(Collectors.toList());

        return new HikeHistoryResponse(dtoList);
    }

    private HikeDetailDTO mapToHikeDetailDTO(Hike hike) {
        return new HikeDetailDTO(
                hike.getId(),
                hike.getHikePercentage(),
                hike.getHikeAmount(),
                hike.getOldSalary(),
                hike.getNewSalary(),
                hike.getEffectiveDate(),
                hike.getPromotionTitle(),
                hike.getComments(),
                hike.getProcessedAt()
        );
    }
}
