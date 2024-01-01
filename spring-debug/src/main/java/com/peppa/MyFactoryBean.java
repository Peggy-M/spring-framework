package com.peppa;

import com.peppa.dto.User;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class MyFactoryBean {
	public static void main(String[] args) {
		ClassPathXmlApplicationContext application = new ClassPathXmlApplicationContext("factoryBean.xml");
		User user = application.getBean(User.class);
		System.out.println(user.hashCode());
		User user2 = application.getBean(User.class);
		System.out.println(user2.hashCode());
	}
}
