
package com.bank.creditbatch.batch;

import com.bank.creditbatch.entity.Client;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;

@Configuration
public class MonthlyRatingUpdateConfig {

    @Bean
    public JpaPagingItemReader<Client> monthlyClientReader(
            EntityManagerFactory entityManagerFactory) {

        return new JpaPagingItemReaderBuilder<Client>()
                .name("monthlyClientReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT c FROM Client c ORDER BY c.clientId")
                .pageSize(100)
                .build();
    }

    @Bean
    public ItemProcessor<Client, Client> monthlyRatingProcessor() {
        return client -> {
            int score = client.getCreditScore();

            // Synthetic demonstration rules only.
            String rating;

            if (score >= 90) {
                rating = "AAA";
            } else if (score >= 80) {
                rating = "AA";
            } else if (score >= 70) {
                rating = "A";
            } else if (score >= 60) {
                rating = "BBB";
            } else if (score >= 50) {
                rating = "BB";
            } else if (score >= 35) {
                rating = "B";
            } else {
                rating = "CCC";
            }

            client.setCreditRating(rating);
            client.setLastReviewDate(LocalDate.now());
            client.setNextReviewDate(LocalDate.now().plusDays(180));

            return client;
        };
    }

    @Bean
    public JpaItemWriter<Client> monthlyClientWriter(
            EntityManagerFactory entityManagerFactory) {

        return new JpaItemWriterBuilder<Client>()
                .entityManagerFactory(entityManagerFactory)
                .usePersist(false)
                .build();
    }

    @Bean
    public Step monthlyRatingUpdateStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            JpaPagingItemReader<Client> monthlyClientReader,
            ItemProcessor<Client, Client> monthlyRatingProcessor,
            JpaItemWriter<Client> monthlyClientWriter) {

        return new StepBuilder(
                "monthlyRatingUpdateStep", jobRepository)
                .<Client, Client>chunk(100, transactionManager)
                .reader(monthlyClientReader)
                .processor(monthlyRatingProcessor)
                .writer(monthlyClientWriter)
                .build();
    }

    @Bean
    public Job monthlyRatingUpdateJob(
            JobRepository jobRepository,
            Step monthlyRatingUpdateStep) {

        return new JobBuilder(
                "monthlyRatingUpdateJob", jobRepository)
                .start(monthlyRatingUpdateStep)
                .build();
    }
}
