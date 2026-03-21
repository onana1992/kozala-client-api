package com.neobank.kozala_client.dto.remote;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Élément du catalogue produits de dépôt (GET /api/client/accounts/deposit-product-catalog sur l’API distante).
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RemoteDepositProductCatalogItemDto {

    private Long id;
    private String code;
    private String name;
    private String description;
    private String category;
    private String status;
    private String currency;
    private BigDecimal minBalance;
    private BigDecimal maxBalance;
    private BigDecimal defaultInterestRate;
    private String createdAt;
    private String updatedAt;

    private List<FeeDto> fees;
    private List<LimitDto> limits;
    private List<InterestRateDto> interestRates;
    private List<PeriodDto> periods;
    private List<PenaltyDto> penalties;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FeeDto {
        private Long id;
        private String feeType;
        private String transactionType;
        private String feeName;
        private BigDecimal feeAmount;
        private BigDecimal feePercentage;
        private String feeCalculationBase;
        private BigDecimal minFee;
        private BigDecimal maxFee;
        private String currency;
        private Boolean isWaivable;
        private String effectiveFrom;
        private String effectiveTo;
        private Boolean isActive;
        private String createdAt;
        private String updatedAt;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LimitDto {
        private Long id;
        private String limitType;
        private String transactionType;
        private BigDecimal limitValue;
        private String currency;
        private String periodType;
        private String effectiveFrom;
        private String effectiveTo;
        private Boolean isActive;
        private String createdAt;
        private String updatedAt;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class InterestRateDto {
        private Long id;
        private String rateType;
        private BigDecimal rateValue;
        private BigDecimal minAmount;
        private BigDecimal maxAmount;
        private Integer minPeriodDays;
        private Integer maxPeriodDays;
        private String calculationMethod;
        private String compoundingFrequency;
        private String effectiveFrom;
        private String effectiveTo;
        private Boolean isActive;
        private String createdAt;
        private String updatedAt;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PeriodDto {
        private Long id;
        private String periodName;
        private Integer periodDays;
        private Integer periodMonths;
        private Integer periodYears;
        private BigDecimal interestRate;
        private BigDecimal minAmount;
        private BigDecimal maxAmount;
        private Boolean isActive;
        private Integer displayOrder;
        private String createdAt;
        private String updatedAt;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PenaltyDto {
        private Long id;
        private String penaltyType;
        private String penaltyName;
        private BigDecimal penaltyAmount;
        private BigDecimal penaltyPercentage;
        private String calculationBase;
        private BigDecimal minPenalty;
        private BigDecimal maxPenalty;
        private String currency;
        private Integer gracePeriodDays;
        private String effectiveFrom;
        private String effectiveTo;
        private Boolean isActive;
        private String createdAt;
        private String updatedAt;
    }
}
