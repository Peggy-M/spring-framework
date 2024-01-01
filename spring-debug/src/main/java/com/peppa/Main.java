package com.peppa;

import com.peppa.dto.User;
import com.peppa.selfEditor.Customer;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Main {
	public static void main(String[] args) {
		ClassPathXmlApplicationContext application = new ClassPathXmlApplicationContext("application.xml");
//		User user = (User) application.getBean("user");
//		System.out.println(user);
//		ClassPathXmlApplicationContext application = new ClassPathXmlApplicationContext("application-${username}.xml");
//		ClassPathXmlApplicationContext application = new MyClassPathXmlApplicationContext("application-${username}.xml");
//		ClassPathXmlApplicationContext editorApplication = new ClassPathXmlApplicationContext("editor.xml");
//		Customer customer= (Customer) editorApplication.getBean("customer");
//		System.out.println(customer);
	}
}