
package com.bank.creditbatch.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.util.UUID;

@Component
@EnableScheduling
@ConditionalOnProperty(
        name = "credit.batch.scheduler.enabled",
        havingValue = "true",
        matchIfMissing = false)
public class MonthlyBatchScheduler {

    private final JobLauncher jobLauncher;
    private final Job importClientsJob;
    private final Job monthlyRatingUpdateJob;

    public MonthlyBatchScheduler(
            JobLauncher jobLauncher,
            @Qualifier("importClientsJob") Job importClientsJob,
            @Qualifier("monthlyRatingUpdateJob") Job monthlyRatingUpdateJob) {

        this.jobLauncher = jobLauncher;
        this.importClientsJob = importClientsJob;
        this.monthlyRatingUpdateJob = monthlyRatingUpdateJob;
    }

    // TEMPORARY TEST: run both jobs every minute
    @Scheduled(cron = "0 * * * * *", zone = "Asia/Kolkata")
    public void runMonthlyUpdate() {
        try {
            JobParameters importParameters =
                    new JobParametersBuilder()
                            .addString("run.id", UUID.randomUUID().toString())
                            .toJobParameters();

            System.out.println("Starting CSV import job...");

            var importExecution =
                    jobLauncher.run(importClientsJob, importParameters);

            System.out.println(
                    "CSV import job status: " + importExecution.getStatus());

            if (importExecution.getStatus().isUnsuccessful()) {
                System.err.println("Import failed; rating job will not run.");
                return;
            }

            JobParameters ratingParameters =
                    new JobParametersBuilder()
                            .addString("reviewMonth", YearMonth.now().toString())
                            .addString("run.id", UUID.randomUUID().toString())
                            .toJobParameters();

            System.out.println("Starting monthly rating update...");

            var ratingExecution =
                    jobLauncher.run(monthlyRatingUpdateJob, ratingParameters);

            System.out.println(
                    "Monthly rating job status: " + ratingExecution.getStatus());

        } catch (Exception exception) {
            System.err.println("Batch execution failed:");
            exception.printStackTrace();
        }
    }
}
