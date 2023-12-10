package com.peppa;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Main {
	public static void main(String[] args) {
		ClassPathXmlApplicationContext application = new ClassPathXmlApplicationContext("application.xml");
	}
}