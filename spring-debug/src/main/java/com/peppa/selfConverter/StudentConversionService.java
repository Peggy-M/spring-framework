package com.peppa.selfConverter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.support.ConversionServiceFactoryBean;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class StudentConversionService {
    @Bean("conversionService")
    public ConversionServiceFactoryBean getStudentConversionService() {
        ConversionServiceFactoryBean conversionServiceFactoryBean = new ConversionServiceFactoryBean();
        StudentConverter studentConverter = new StudentConverter();
        Set<StudentConverter> maps = new HashSet<>();
        maps.add(studentConverter);
        conversionServiceFactoryBean.setConverters(maps);
        return conversionServiceFactoryBean;
    }
}
