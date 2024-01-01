package com.peppa;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Component
@PropertySource("classpath:db.properties")
@Configuration
public class PersonService {

	@Value("#{jdbc.url}")
	private String url;

}
