package com.peppa.myFactoryBean;

import com.peppa.dto.User;
import org.springframework.beans.factory.FactoryBean;

public class MyFactoryBean implements FactoryBean<User> {
	@Override
	public User getObject() throws Exception {
		User user = new User(); // 这个对象是我们自己 new 不在 一二三 级缓存当中
		user.setName("卡卡罗特");
		user.setId("9527");
		user.setEmail("9527666@xx.com");
		user.setPassword("阿巴阿巴");
		return user;
	}

	@Override
	public Class<?> getObjectType() {
		return User.class;
	}

}
