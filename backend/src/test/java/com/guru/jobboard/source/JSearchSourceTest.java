package com.guru.jobboard.source;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guru.jobboard.entity.Preference;
import com.guru.jobboard.service.JobSites;

class JSearchSourceTest {

    @Test
    void parsesJobsAndUsesPublisherAsSource() throws Exception {
        String json = """
                {"status":"OK","data":[
                  {"job_id":"abc==","job_title":"Java Developer","employer_name":"Acme",
                   "job_publisher":"LinkedIn","job_apply_link":"https://acme.example/apply",
                   "apply_options":[{"publisher":"LinkedIn","apply_link":"https://www.linkedin.com/jobs/view/123","is_direct":false}],
                   "job_description":"Spring Boot and React","job_is_remote":false,
                   "job_city":"Bengaluru","job_state":"Karnataka","job_country":"IN",
                   "job_posted_at_datetime_utc":"2026-10-15T10:00:00.000Z",
                   "job_offer_expiration_datetime_utc":"2026-11-15T00:00:00.000Z",
                   "job_min_salary":800000,"job_max_salary":1200000,"job_salary_period":"YEAR","job_employment_type":"FULLTIME"},
                  {"job_id":"def==","job_title":"Backend Engineer","employer_name":"Beta",
                   "job_publisher":"Naukri.com","job_apply_link":"https://www.naukri.com/job-listings-1"}
                ]}""";
        List<FetchedJob> jobs = JSearchSource.parse(new ObjectMapper().readTree(json));
        assertEquals(2, jobs.size());
        FetchedJob first = jobs.get(0);
        assertEquals("linkedin", first.source());
        assertEquals("https://www.linkedin.com/jobs/view/123", first.url());
        assertEquals("Bengaluru, Karnataka, IN", first.location());
        assertEquals(Instant.parse("2026-11-15T00:00:00Z"), first.expiresAt());
        assertTrue(first.salary().contains("800,000"));
        assertEquals("naukri", jobs.get(1).source());
    }

    @Test
    void queryUsesDesiredRoleAndLocation() {
        Preference p = new Preference(1L);
        p.setDesiredRole("Java Developer");
        p.setLocation("Bangalore");
        assertEquals("Java Developer jobs in Bangalore", JSearchSource.query(p));
        p.setDesiredRole(null);
        p.setKeywords("java, spring boot, react");
        assertEquals("java spring boot jobs in Bangalore", JSearchSource.query(p));
    }

    @Test
    void recognisesJobSites() {
        assertEquals("linkedin", JobSites.fromUrl("https://www.linkedin.com/jobs/view/123/"));
        assertEquals("naukri", JobSites.fromUrl("https://www.naukri.com/job-listings-java-123"));
        assertEquals("indeed", JobSites.fromUrl("https://in.indeed.com/viewjob?jk=1"));
        assertEquals("wellfound", JobSites.fromUrl("https://wellfound.com/jobs/123-java"));
        assertEquals("careers.acme.com", JobSites.fromUrl("https://careers.acme.com/1"));
        assertEquals("glassdoor", JobSites.fromPublisher("Glassdoor"));
    }
}
