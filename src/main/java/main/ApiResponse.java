package main;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public sealed interface ApiResponse
        permits ApiResponse.Success, ApiResponse.Error, ApiResponse.Initial {

    DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    LocalTime updateTime();

    default String updateTimeStr() {

        return updateTime().format(TIME_FORMATTER);
    }

    record Success(BigDecimal spend, LocalTime updateTime)
            implements ApiResponse {

        public Success(BigDecimal spend) {

            this(spend, LocalTime.now());
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
