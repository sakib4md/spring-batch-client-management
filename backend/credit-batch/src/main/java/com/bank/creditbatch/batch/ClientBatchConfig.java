
package com.bank.creditbatch.batch;

import com.bank.creditbatch.entity.Client;
import com.bank.creditbatch.repository.ClientRepository;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.LocalDate;

@Configuration
public class ClientBatchConfig {

    @Bean
    public FlatFileItemReader<Client> clientReader(
            @Value("${credit.batch.csv-resource:classpath:data/synthetic_credit_clients_10000.csv}")
            Resource csvResource) {

        DelimitedLineTokenizer tokenizer =
                new DelimitedLineTokenizer();

        tokenizer.setDelimiter(",");
        tokenizer.setQuoteCharacter('"');

        tokenizer.setNames(
                "client_id",
                "client_name",
                "industry",
                "country",
                "annual_revenue_inr",
                "credit_score",
                "credit_rating",
                "credit_exposure_inr",
                "package",
                "onboarding_status",
                "last_review_date",
                "next_review_date"
        );

        FieldSetMapper<Client> mapper = fields -> {
            Client client = new Client();

            client.setClientId(
                    fields.readString("client_id"));
            client.setClientName(
                    fields.readString("client_name"));
            client.setIndustry(
                    fields.readString("industry"));
            client.setCountry(
                    fields.readString("country"));

            client.setAnnualRevenueInr(
                    new BigDecimal(
                            fields.readString("annual_revenue_inr")));

            client.setCreditScore(
                    fields.readInt("credit_score"));
            client.setCreditRating(
                    fields.readString("credit_rating"));

            client.setCreditExposureInr(
                    new BigDecimal(
                            fields.readString("credit_exposure_inr")));

            client.setPackageName(
                    fields.readString("package"));
            client.setOnboardingStatus(
                    fields.readString("onboarding_status"));

            client.setLastReviewDate(
                    LocalDate.parse(
                            fields.readString("last_review_date")));

            client.setNextReviewDate(
                    LocalDate.parse(
                            fields.readString("next_review_date")));

            return client;
        };

        DefaultLineMapper<Client> lineMapper =
                new DefaultLineMapper<>();

        lineMapper.setLineTokenizer(tokenizer);
        lineMapper.setFieldSetMapper(mapper);

        return new FlatFileItemReaderBuilder<Client>()
                .name("clientReader")
                .resource(csvResource)
                .linesToSkip(1)
                .lineMapper(lineMapper)
                .build();
    }

    @Bean
    public ItemProcessor<Client, Client> clientProcessor() {

        return client -> {
            if (client.getClientId() == null
                    || client.getClientId().isBlank()) {
                throw new IllegalArgumentException(
                        "Client ID cannot be empty");
            }

            if (client.getCreditScore() == null
                    || client.getCreditScore() < 0
                    || client.getCreditScore() > 100) {
                throw new IllegalArgumentException(
                        "Invalid credit score for client: "
                                + client.getClientId());
            }

            return client;
        };
    }

    @Bean
    public ItemWriter<Client> clientWriter(
            ClientRepository clientRepository) {

        return items -> {
            for (Client incoming : items) {

                Client existing = clientRepository
                        .findById(incoming.getClientId())
                        .orElse(null);

                if (existing == null) {
                    // New client: insert.
                    clientRepository.save(incoming);
                } else {
                    // Existing client: update its fields.
                    existing.setClientName(
                            incoming.getClientName());
                    existing.setIndustry(
                            incoming.getIndustry());
                    existing.setCountry(
                            incoming.getCountry());
                    existing.setAnnualRevenueInr(
                            incoming.getAnnualRevenueInr());
                    existing.setCreditScore(
                            incoming.getCreditScore());
                    existing.setCreditRating(
                            incoming.getCreditRating());
                    existing.setCreditExposureInr(
                            incoming.getCreditExposureInr());
                    existing.setPackageName(
                            incoming.getPackageName());
                    existing.setOnboardingStatus(
                            incoming.getOnboardingStatus());
                    existing.setLastReviewDate(
                            incoming.getLastReviewDate());
                    existing.setNextReviewDate(
                            incoming.getNextReviewDate());

                    clientRepository.save(existing);
                }
            }
        };
    }

    @Bean
    public Step importClientsStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            FlatFileItemReader<Client> clientReader,
            ItemProcessor<Client, Client> clientProcessor,
            ItemWriter<Client> clientWriter) {

        return new StepBuilder(
                "importClientsStep", jobRepository)
                .<Client, Client>chunk(
                        100, transactionManager)
                .reader(clientReader)
                .processor(clientProcessor)
                .writer(clientWriter)
                .build();
    }

    @Bean
    public Job importClientsJob(
            JobRepository jobRepository,
            Step importClientsStep) {

        return new JobBuilder(
                "importClientsJob", jobRepository)
                .start(importClientsStep)
                .build();
    }
}
