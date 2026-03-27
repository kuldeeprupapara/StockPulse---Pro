package com.stockmarket.interfaces.impl;

import com.stockmarket.config.APIConfig;
import com.stockmarket.interfaces.Validator;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

@Service
public class ApiConfigValidator implements Validator<APIConfig> {
    @Override
    public boolean validate(APIConfig argument) {

        return false;
    }
    private boolean checkLocalDate(APIConfig argument) {
        LocalDate current = LocalDate.now();
        LocalDate previous = LocalDate.now().minusDays(1);
        if(current.isAfter(previous)){
            return false;
        }
        return isHoliday(argument);
    }
    private boolean isHoliday(APIConfig argument) {
        return true;
    }

    private boolean isWeekendDay(APIConfig argument) {
        return true;
    }
}
