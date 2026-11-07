
package com.bank.creditbatch.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "clients")
public class Client {

    @Id
    @Column(name = "client_id", length = 20)
    private String clientId;

    @Column(name = "client_name", nullable = false)
    private String clientName;

    private String industry;
    private String country;

    @Column(precision = 19, scale = 2)
    private BigDecimal annualRevenueInr;

    private Integer creditScore;
    private String creditRating;

    @Column(precision = 19, scale = 2)
    private BigDecimal creditExposureInr;

    private String packageName;
    private String onboardingStatus;
    private LocalDate lastReviewDate;
    private LocalDate nextReviewDate;

    public Client() {}

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }

    public String getIndustry() { return industry; }
    public void setIndustry(String industry) { this.industry = industry; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public BigDecimal getAnnualRevenueInr() { return annualRevenueInr; }
    public void setAnnualRevenueInr(BigDecimal value) { this.annualRevenueInr = value; }

    public Integer getCreditScore() { return creditScore; }
    public void setCreditScore(Integer creditScore) { this.creditScore = creditScore; }

    public String getCreditRating() { return creditRating; }
    public void setCreditRating(String creditRating) { this.creditRating = creditRating; }

    public BigDecimal getCreditExposureInr() { return creditExposureInr; }
    public void setCreditExposureInr(BigDecimal value) { this.creditExposureInr = value; }

    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }

    public String getOnboardingStatus() { return onboardingStatus; }
    public void setOnboardingStatus(String onboardingStatus) { this.onboardingStatus = onboardingStatus; }

    public LocalDate getLastReviewDate() { return lastReviewDate; }
    public void setLastReviewDate(LocalDate value) { this.lastReviewDate = value; }

    public LocalDate getNextReviewDate() { return nextReviewDate; }
    public void setNextReviewDate(LocalDate value) { this.nextReviewDate = value; }
}
