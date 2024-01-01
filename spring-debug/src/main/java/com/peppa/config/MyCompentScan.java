package com.peppa.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

//@Configuration
//@ComponentScan("com.peppa")
public class MyCompentScan {

	@ComponentScan("com.peppa")
	@Configuration
	class InnerClass{

	}
}
