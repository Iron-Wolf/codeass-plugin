package main;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.jetbrains.annotations.Nullable;

public sealed interface ApiResponse
        permits ApiResponse.Success, ApiResponse.Error, ApiResponse.Initial {

    DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    LocalTime updateTime();

    default String updateTimeStr() {

        return updateTime().format(TIME_FORMATTER);
    }

    record ModelUsage(String model, int requests, BigDecimal spend, long totalTokens) {

        public String spendStr() {

            return spend.setScale(2, RoundingMode.HALF_UP).toPlainString();
        }
    }

    record HistoryCycle(
            String cycle,
            BigDecimal spend,
            int requests,
            long totalTokens,
            List<ModelUsage> byModel
    ) {

        public String spendStr() {

            return spend.setScale(2, RoundingMode.HALF_UP).toPlainString();
        }
    }

    record DashboardDetails(
            BigDecimal maxBudget,
            int totalRequests,
            long totalTokens,
            String expires,
            List<ModelUsage> byModel,
            List<HistoryCycle> history
    ) {

        public DashboardDetails(
                BigDecimal maxBudget,
                int totalRequests,
                long totalTokens,
                String expires,
                List<ModelUsage> byModel
        ) {

            this(maxBudget, totalRequests, totalTokens, expires, byModel, List.of());
        }
    }

    record Success(BigDecimal spend, LocalTime updateTime, @Nullable DashboardDetails details)
            implements ApiResponse {

        public Success(BigDecimal spend) {

            this(spend, LocalTime.now(), null);
        }

        public Success(BigDecimal spend, LocalTime updateTime) {

            this(spend, updateTime, null);
        }

        public Success(BigDecimal spend, @Nullable DashboardDetails details) {

            this(spend, LocalTime.now(), details);
        }

        public String spendStr() {

            return spend.setScale(2, RoundingMode.HALF_UP).toPlainString();
        }
    }

    record Error(int httpStatus,
                 String message,
                 LocalTime updateTime)
            implements ApiResponse {

        public Error(int httpStatus, String message) {

            this(httpStatus, message, LocalTime.now());
        }
    }

    record Initial(LocalTime updateTime)
            implements ApiResponse {

        public Initial() {

            this(LocalTime.now());
        }
    }
}
