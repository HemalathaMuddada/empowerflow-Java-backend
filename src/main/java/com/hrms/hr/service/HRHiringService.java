package com.hrms.hr.service;

import com.hrms.core.entity.User;
import com.hrms.core.repository.UserRepository;
import com.hrms.hr.entity.HiringResume;
import com.hrms.hr.payload.request.HRResumeUploadRequest;
import com.hrms.hr.payload.response.HiringResumeDTO;
import com.hrms.hr.payload.response.HiringResumeListResponse;
import com.hrms.hr.repository.HiringResumeRepository;
import com.hrms.security.service.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

// Assuming ResourceNotFoundException & BadRequestException are defined (e.g., in HREmployeeService or a common place)

@Service
public class HRHiringService {

    @Autowired
    private HiringResumeRepository hiringResumeRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public HiringResumeDTO uploadResumeLink(HRResumeUploadRequest request, UserDetailsImpl hrUserDetails) {
        User hrUser = userRepository.findById(hrUserDetails.getId())
                .orElseThrow(() -> new UsernameNotFoundException("HR User not found: " + hrUserDetails.getUsername()));

        HiringResume resume = new HiringResume();
        resume.setCandidateName(request.getCandidateName());
        resume.setResumeLink(request.getResumeLink());
        resume.setSkills(request.getSkills());
        resume.setCategory(request.getCategory());
        resume.setNotes(request.getNotes());
        resume.setUploadedBy(hrUser);
        // resume.setUploadedAt(LocalDateTime.now()); // Handled by @CreatedDate

        HiringResume savedResume = hiringResumeRepository.save(resume);
        return mapToHiringResumeDTO(savedResume);
    }

    @Transactional(readOnly = true)
    public HiringResumeListResponse listResumes(String filterBySkill, String filterByCategory, UserDetailsImpl hrUserDetails) {
        // Permissions: For now, any HR can see all resumes. Company scoping could be added.
        // User hrUser = userRepository.findById(hrUserDetails.getId()).orElseThrow(...);
        // Company hrCompany = hrUser.getCompany(); // And filter by company if resumes are company-specific.

        Specification<HiringResume> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(filterByCategory)) {
                predicates.add(criteriaBuilder.equal(root.get("category"), filterByCategory));
            }
            if (StringUtils.hasText(filterBySkill)) {
                // Using lower to make search case-insensitive, assumes DB supports it or use DB specific functions
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("skills")), "%" + filterBySkill.toLowerCase() + "%"));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        List<HiringResume> resumes = hiringResumeRepository.findAll(spec);
        // Could add Pageable here for pagination

        List<HiringResumeDTO> dtoList = resumes.stream()
                .map(this::mapToHiringResumeDTO)
                .collect(Collectors.toList());

        return new HiringResumeListResponse(dtoList);
    }


    private HiringResumeDTO mapToHiringResumeDTO(HiringResume resume) {
        String uploaderName = (resume.getUploadedBy() != null) ?
                (resume.getUploadedBy().getFirstName() + " " + resume.getUploadedBy().getLastName()) : "N/A";
        return new HiringResumeDTO(
                resume.getId(),
                resume.getCandidateName(),
                resume.getResumeLink(),
                resume.getSkills(),
                resume.getCategory(),
                resume.getNotes(),
                uploaderName,
                resume.getUploadedAt()
        );
    }
}
