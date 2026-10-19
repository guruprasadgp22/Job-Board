package com.guru.jobboard.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.guru.jobboard.dto.EmployerDashboardDto;
import com.guru.jobboard.dto.PostingDto;
import com.guru.jobboard.entity.ApplicantStatus;
import com.guru.jobboard.entity.JobApplication;
import com.guru.jobboard.entity.Posting;
import com.guru.jobboard.entity.User;
import com.guru.jobboard.repository.JobApplicationRepository;
import com.guru.jobboard.repository.PostingRepository;
import com.guru.jobboard.repository.UserRepository;

/** Builds an employer's dashboard from their own postings and the applications to them. */
@Service
public class EmployerDashboardService {

    private final PostingRepository postings;
    private final JobApplicationRepository applications;
    private final UserRepository users;
    private final ApplicationService applicationService;

    public EmployerDashboardService(PostingRepository postings, JobApplicationRepository applications,
                                    UserRepository users, ApplicationService applicationService) {
        this.postings = postings;
        this.applications = applications;
        this.users = users;
        this.applicationService = applicationService;
    }

    @Transactional(readOnly = true)
    public EmployerDashboardDto build(long employerId) {
        User me = users.findById(employerId).orElseThrow();
        List<Posting> mine = postings.findByEmployerIdOrderByCreatedAtDesc(employerId);
        Map<Long, Posting> byId = mine.stream().collect(Collectors.toMap(Posting::getId, Function.identity()));
        List<JobApplication> apps = mine.isEmpty() ? List.of() : applications.findByPostingIdIn(byId.keySet());
        Instant now = Instant.now();

        Map<String, Long> byStatus = new LinkedHashMap<>();
        Arrays.stream(ApplicantStatus.values()).forEach(s -> byStatus.put(s.name(), 0L));
        apps.forEach(a -> byStatus.merge(a.getStatus().name(), 1L, Long::sum));

        long thisWeek = apps.stream().filter(a -> a.getAppliedAt().isAfter(now.minus(Duration.ofDays(7)))).count();
        long open = mine.stream().filter(Posting::acceptsApplications).count();

        Map<Long, List<JobApplication>> perPosting = apps.stream().collect(Collectors.groupingBy(JobApplication::getPostingId));
        List<PostingDto> openDtos = mine.stream()
                .filter(Posting::acceptsApplications)
                .map(p -> {
                    List<JobApplication> list = perPosting.getOrDefault(p.getId(), List.of());
                    long fresh = list.stream().filter(a -> a.getStatus() == ApplicantStatus.APPLIED).count();
                    return PostingDto.forEmployer(p, false, list.size(), fresh);
                })
                .toList();
        List<PostingDto> top = openDtos.stream()
                .sorted(Comparator.comparing(PostingDto::applicantCount, Comparator.reverseOrder()))
                .limit(5).toList();
        List<PostingDto> closingSoon = openDtos.stream()
                .filter(p -> p.deadline() != null && p.deadline().isBefore(now.plus(Duration.ofDays(7))))
                .sorted(Comparator.comparing(PostingDto::deadline))
                .limit(5).toList();

        List<JobApplication> recent = apps.stream()
                .sorted(Comparator.comparing(JobApplication::getAppliedAt).reversed())
                .limit(6).toList();

        return new EmployerDashboardDto(me.getName(), me.getOrganization(), open, mine.size() - open, apps.size(),
                thisWeek, byStatus, applicationService.toDtos(recent, byId, false), top, closingSoon);
    }
}
