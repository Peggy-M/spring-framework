package com.peppa;

import com.peppa.selfEditor.MyBeanPostProcessor;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
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
//		getEnvironment().setRequiredProperties("peppa");
	}

	@Override
	protected void customizeBeanFactory(DefaultListableBeanFactory beanFactory) {
		super.setAllowBeanDefinitionOverriding(false);
		super.setAllowCircularReferences(false);
		super.customizeBeanFactory(beanFactory);
		super.addBeanFactoryPostProcessor(new MyBeanPostProcessor());
	}

	@Override
	protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		System.out.println("调用了自定义实现的 postProcessBeanFactory");
		super.postProcessBeanFactory(beanFactory);
	}
}
