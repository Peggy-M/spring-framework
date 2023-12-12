package com.peppa;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * 自定义 ClassPathXmlApplicationContext 继承 AbstractApplicationContext 实现 initPropertySources(); 修改环境属性
 */
public class MyClassPathXmlApplicationContext extends ClassPathXmlApplicationContext {

	public MyClassPathXmlApplicationContext(String... configLocation) {
		super(configLocation);
	}

	@Override
	protected void initPropertySources() {
		System.out.println("调用扩展的 intiPropertySources 方法");
		// 获取当前系统环境并添加 peppa 属性
		getEnvironment().setRequiredProperties("peppa");
	}
}
