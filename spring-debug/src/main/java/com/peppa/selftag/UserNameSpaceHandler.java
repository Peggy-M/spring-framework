package com.peppa.selftag;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

public class UserNameSpaceHandler extends NamespaceHandlerSupport {
	@Override
	public void init() {
		//添加 user 标签
		registerBeanDefinitionParser("user", new UserBeanDefinitionParser());
	}
}
