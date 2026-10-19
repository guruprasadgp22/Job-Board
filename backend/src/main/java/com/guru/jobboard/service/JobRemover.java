package com.guru.jobboard.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.guru.jobboard.entity.Job;
import com.guru.jobboard.entity.RemovedJob;
import com.guru.jobboard.repository.JobRepository;
import com.guru.jobboard.repository.RemovedJobRepository;

/** Deletes a job (with its application data) and remembers it so syncing does not bring it back. */
@Component
public class JobRemover {

    private final JobRepository jobs;
    private final RemovedJobRepository removed;

    public JobRemover(JobRepository jobs, RemovedJobRepository removed) {
        this.jobs = jobs;
        this.removed = removed;
    }

    @Transactional
    public void remove(Job job, String reason) {
        if (!removed.existsByUserIdAndSourceAndExternalId(job.getUserId(), job.getSource(), job.getExternalId())) {
            removed.save(new RemovedJob(job.getUserId(), job.getSource(), job.getExternalId(),
                    JobSyncService.truncate(reason, 200)));
        }
        jobs.deleteById(job.getId());
    }
}
