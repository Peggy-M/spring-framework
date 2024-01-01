/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.lang.Nullable;

/**
 * Delegate for AbstractApplicationContext's post-processor handling.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 4.0
 */
final class PostProcessorRegistrationDelegate {

	private PostProcessorRegistrationDelegate() {
	}


	public static void invokeBeanFactoryPostProcessors(ConfigurableListableBeanFactory beanFactory, List<BeanFactoryPostProcessor> beanFactoryPostProcessors) {

		// Invoke BeanDefinitionRegistryPostProcessors first, if any.
		Set<String> processedBeans = new HashSet<>(); // 已经处理过的 BFPP 存储在 processedBeans 防止重复执行

		// 判断 beanDefactory 是否是 BeanDefinitioRegistry 类型, 此处是 DefaultListableBeanFactory ,实现了 BeanDefinitionRegistry 接口,所以为 true
		if (beanFactory instanceof BeanDefinitionRegistry) {
			BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory; // 类型转化
			// BeanDefinitionRegistryPostProcessor 是 BeanFactoryPostProcessor 的子接口, 所以 registryProcessors 集合当中是包含 regularPostProcessors 的。
			List<BeanFactoryPostProcessor> regularPostProcessors = new ArrayList<>(); // BeanFactoryPostProcessor 当中的 postProcessBeanFactory() 方法最终执行的对象 beanFactory
			List<BeanDefinitionRegistryPostProcessor> registryProcessors = new ArrayList<>(); // BeanDefinitionRegistryPostProcessor 当中 postProcessBeanDefinitionRegistry() 方法最终执行的对象是 registry

			for (BeanFactoryPostProcessor postProcessor : beanFactoryPostProcessors) { // 将 BFPP 进区分
				if (postProcessor instanceof BeanDefinitionRegistryPostProcessor) {
					// 这里做出一个类型转化将 BFPP 强转成 BDRPP
					BeanDefinitionRegistryPostProcessor registryProcessor =
							(BeanDefinitionRegistryPostProcessor) postProcessor;
					// 这里就可以顺便提前直接执行 BDRPP 下的 postProcessBeanDefinitionRegistry() 方法
					registryProcessor.postProcessBeanDefinitionRegistry(registry);
					// 如果时 BDRPP 添加到 registryProcessors 集合,用于后续执行 postProcessBeanFactory() 方法
					registryProcessors.add(registryProcessor);
				}
				else {
					// 否则就是 BFPP 添加到 regularPostProcessors 集合当中, 用于后续执行 postProcessBeanFactory() 方法
					regularPostProcessors.add(postProcessor);
				}
			}

			// Do not initialize FactoryBeans here: We need to leave all regular beans
			// uninitialized to let the bean factory post-processors apply to them!
			// Separate between BeanDefinitionRegistryPostProcessors that implement
			// PriorityOrdered, Ordered, and the rest.
			//===================== 从这里开始执行处理内部的 BFPP 与外部 [自定义的外部] 的没有关系 ====================
			// 用于保存本次要执行的 BeanDefinitionRegistryPostProcessor
			List<BeanDefinitionRegistryPostProcessor> currentRegistryProcessors = new ArrayList<>();

			// First, invoke the BeanDefinitionRegistryPostProcessors that implement PriorityOrdered.
			// 首先执行的是从容器中取出的实现 PriorityOrdered 的 RDRPP
			// 从内部的容器当中找到实现了 BeanDefinitionRegistryPostProcessor 接口的 bean 的 beanName
			String[] postProcessorNames =
					beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
			// 遍历符合要求的所有内部 BDRPP
			for (String ppName : postProcessorNames) {
				// 检测该 BDRPP 是否实现 PriorityOrdered 接口 [主要用于后续的优先级排序,按照顺序执行 BDRPP 的操作]
				if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
					// 将获取到的 bean 添加到当前需要进行处理的 BDRPP 集合当中
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					// 将要被执行的 BDRPP 的名称添加到 processedBeans [处理Beans] 的集合当中, 避免后续重复执行
					processedBeans.add(ppName);
				}
			}
			// 将当前需要执行的 BDRPP 进行优先级的排序
			sortPostProcessors(currentRegistryProcessors, beanFactory);
			// 将当前内部需要执行的 BDRPP 添加到之前处理外部 BDRPP 的集合 registryProcessors 当中，用于最终 BDRPP  下的父接口 postProcessBeanFactory() 方法执行
			registryProcessors.addAll(currentRegistryProcessors);
			// 与前面处理外部的流程一样,这里先执行 BDRPP 下的 postProcessBeanDefinitionRegistry() 方法
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
			// 清空内部当前执行的 BDRPP 集合类
			// [记住：这里还有没有实现 PriorityOrdered 接口的 ,内部 BDRPP 还没有执行]
			currentRegistryProcessors.clear();

			//========================================重复执行=================================================
			// Next, invoke the BeanDefinitionRegistryPostProcessors that implement Ordered.
			// 接下来执行的是从容器当中取出的实现了 Ordered 接口而 BDRPP (与 PriorityOrdered处理一致 )
			// 调用所有实现 Ordered 接口的 BDRPP 内部实现类
			// 找到所有实现 BeanDefinitionRegistryPostProcessor 接口 bean 的 beanName
			// 【此处之所以需要重复查找的原因在于上面 invokeBeanDefinitionRegistryPostProcessors()方法在调用的时候可能会新增其他的 BDRPP】
			postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
			for (String ppName : postProcessorNames) {
				// 判断从内部重新获取 BDRPP 是否是已经执行过的 bean,如果时就不再重复执行
				if (!processedBeans.contains(ppName) && beanFactory.isTypeMatch(ppName, Ordered.class)) {
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					processedBeans.add(ppName);
				}
			}
			sortPostProcessors(currentRegistryProcessors, beanFactory); // 排序
			registryProcessors.addAll(currentRegistryProcessors); // 添加
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry); // 执行
			currentRegistryProcessors.clear(); // 清除

			// Finally, invoke all other BeanDefinitionRegistryPostProcessors until no further ones appear.
			// 最后,调用剩下所有的 BeanDefinitionRegistryPostProcessors [没有实现排序的]
			boolean reiterate = true;
			while (reiterate) {
				reiterate = false;
				postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
				for (String ppName : postProcessorNames) {
					// 跳过之前已经执行过的 Bean
					if (!processedBeans.contains(ppName)) {
						currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
						processedBeans.add(ppName);
						reiterate = true;
					}
				}
				sortPostProcessors(currentRegistryProcessors, beanFactory); // 对于没有实现排序接口返回 Internet 的对大值,默认最后
				registryProcessors.addAll(currentRegistryProcessors);
				invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
				currentRegistryProcessors.clear();
			}

			// Now, invoke the postProcessBeanFactory callback of all processors handled so far.
			// 调用执行所有 BDRPP 里面的  postProcessBeanFactory() 方法 【将 BDRPP 转为 BFPP】
			invokeBeanFactoryPostProcessors(registryProcessors, beanFactory);
			// 调用处理所有 BFPP  里面的  postProcessBeanFactory() 方法
			invokeBeanFactoryPostProcessors(regularPostProcessors, beanFactory);
		}

		else {
			// Invoke factory processors registered with the context instance.
			// 如果 beanFactory 不归属于 BeanDefinitionRegistry 类型, 那么直接执行 postProcessBeanFactory
			// 【这里也就是说不存在 BRDPP 包含 BFPP 的关系】
			invokeBeanFactoryPostProcessors(beanFactoryPostProcessors, beanFactory);
		}

		// ================================================
		// 到目前为止,入参 BFPP [beanFactory 中的 bean 都是我们外部通过解析 xml 配置文件创建的] 和容器中的所有 BDRPP 都已经处理完毕
		// 而接下来就开始处理容器中的所有的 BFPP
		// ==========================================================
		// Do not initialize FactoryBeans here: We need to leave all regular beans
		// uninitialized to let the bean factory post-processors apply to them!
		// 找到所有实现 BFPP 的接口内部 beanName
		String[] postProcessorNames =
				beanFactory.getBeanNamesForType(BeanFactoryPostProcessor.class, true, false);

		// Separate between BeanFactoryPostProcessors that implement PriorityOrdered,
		// Ordered, and the rest.
		List<BeanFactoryPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
		List<String> orderedPostProcessorNames = new ArrayList<>();
		List<String> nonOrderedPostProcessorNames = new ArrayList<>();
		for (String ppName : postProcessorNames) {
			// 判断之前已经处理过的 bean 中是否包含内部获取到的 BFPP
			// [之前的 BDRPP 当中包含 BFPP ,两个接口是父子关系,因此在这一步还是会扫描到 BDRPP,因此需要过滤]
			if (processedBeans.contains(ppName)) {
				// skip - already processed in first phase above
			}
			else if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
				// 实现 PriorityOrdered 接口添加到有 PriorityOrdered 接口的集合当中
				priorityOrderedPostProcessors.add(beanFactory.getBean(ppName, BeanFactoryPostProcessor.class));
			}
			else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
				// 实现 Ordered 接口添加到有 Ordered 接口的集合当中
				orderedPostProcessorNames.add(ppName);
			}
			else {
				// 没有实现的添加到无排序的集合当中
				nonOrderedPostProcessorNames.add(ppName);
			}
		}

		// First, invoke the BeanFactoryPostProcessors that implement PriorityOrdered.
		// 排序 PriorityOrdered 接口集合
		sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
		// 执行 BFPP
		invokeBeanFactoryPostProcessors(priorityOrderedPostProcessors, beanFactory);

		// Next, invoke the BeanFactoryPostProcessors that implement Ordered.
		List<BeanFactoryPostProcessor> orderedPostProcessors = new ArrayList<>(orderedPostProcessorNames.size());
		for (String postProcessorName : orderedPostProcessorNames) {
			orderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		// 排序执行
		sortPostProcessors(orderedPostProcessors, beanFactory);
		invokeBeanFactoryPostProcessors(orderedPostProcessors, beanFactory);

		// Finally, invoke all other BeanFactoryPostProcessors.
		List<BeanFactoryPostProcessor> nonOrderedPostProcessors = new ArrayList<>(nonOrderedPostProcessorNames.size());
		// 循环添加
		for (String postProcessorName : nonOrderedPostProcessorNames) {
			nonOrderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		// 执行
		invokeBeanFactoryPostProcessors(nonOrderedPostProcessors, beanFactory);

		// Clear cached merged bean definitions since the post-processors might have
		// modified the original metadata, e.g. replacing placeholders in values...
		// 清理所有的缓存
		beanFactory.clearMetadataCache();
	}

	public static void registerBeanPostProcessors(
			ConfigurableListableBeanFactory beanFactory, AbstractApplicationContext applicationContext) {
		// 找到所有实现了 BeanPostProcessor 的 bean
		String[] postProcessorNames = beanFactory.getBeanNamesForType(BeanPostProcessor.class, true, false);

		// Register BeanPostProcessorChecker that logs an info message when
		// a bean is created during BeanPostProcessor instantiation, i.e. when
		// a bean is not eligible for getting processed by all BeanPostProcessors.
		// 记录下 BeanPostProcessor 的目标计数
		// 这里进行 beanPostProcessor 加 1 并不是因为后面的 new ApplicationListenerDetector() 而是在 prepareBeanFactory(beanFactory) 当中已经添加过 new ApplicationListenerDetector() 了
		int beanProcessorTargetCount = beanFactory.getBeanPostProcessorCount() + 1 + postProcessorNames.length;
		// BeanPostProcessorChecker [主要用于记录信息]
		beanFactory.addBeanPostProcessor(new BeanPostProcessorChecker(beanFactory, beanProcessorTargetCount));

		// Separate between BeanPostProcessors that implement PriorityOrdered,
		// Ordered, and the rest.
		List<BeanPostProcessor> priorityOrderedPostProcessors = new ArrayList<>(); // 实现了 PriorityOrdered 接口的集合
		List<BeanPostProcessor> internalPostProcessors = new ArrayList<>(); // 存放 spring 内部的 BeanPostProcessor
		List<String> orderedPostProcessorNames = new ArrayList<>(); // 定义存放实现 Ordered 接口的集合
		List<String> nonOrderedPostProcessorNames = new ArrayList<>(); // 定义存放普通的 BeanPostProcessor 的 name 集合
		for (String ppName : postProcessorNames) {
			if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
				BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
				priorityOrderedPostProcessors.add(pp);
				if (pp instanceof MergedBeanDefinitionPostProcessor) {
					internalPostProcessors.add(pp);
				}
			}
			else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
				orderedPostProcessorNames.add(ppName);
			}
			else {
				nonOrderedPostProcessorNames.add(ppName);
			}
		}

		// First, register the BeanPostProcessors that implement PriorityOrdered.
		// 对实现了 PriorityOrdered 接口的 BeanPostProcessors 进行排序
		sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
		// 注册实现了 PriorityOrdered 接口的 BeanPostProcessors 的实例,添加到 beanFactory 当中去
		registerBeanPostProcessors(beanFactory, priorityOrderedPostProcessors);

		// Next, register the BeanPostProcessors that implement Ordered.
		// 注册所有实现了 Ordered 的 beanPostProcessors
		List<BeanPostProcessor> orderedPostProcessors = new ArrayList<>(orderedPostProcessorNames.size());
		for (String ppName : orderedPostProcessorNames) {
			BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
			orderedPostProcessors.add(pp);
			if (pp instanceof MergedBeanDefinitionPostProcessor) {
				internalPostProcessors.add(pp);
			}
		}
		sortPostProcessors(orderedPostProcessors, beanFactory);
		registerBeanPostProcessors(beanFactory, orderedPostProcessors);

		// Now, register all regular BeanPostProcessors.
		List<BeanPostProcessor> nonOrderedPostProcessors = new ArrayList<>(nonOrderedPostProcessorNames.size());
		for (String ppName : nonOrderedPostProcessorNames) {
			BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
			nonOrderedPostProcessors.add(pp);
			if (pp instanceof MergedBeanDefinitionPostProcessor) {
				internalPostProcessors.add(pp);
			}
		}
		registerBeanPostProcessors(beanFactory, nonOrderedPostProcessors);

		// Finally, re-register all internal BeanPostProcessors.
		sortPostProcessors(internalPostProcessors, beanFactory);
		registerBeanPostProcessors(beanFactory, internalPostProcessors);

		// Re-register post-processor for detecting inner beans as ApplicationListeners,
		// moving it to the end of the processor chain (for picking up proxies etc).
		beanFactory.addBeanPostProcessor(new ApplicationListenerDetector(applicationContext));
	}

	private static void sortPostProcessors(List<?> postProcessors, ConfigurableListableBeanFactory beanFactory) {
		// Nothing to sort?
		if (postProcessors.size() <= 1) {
			return;
		}
		Comparator<Object> comparatorToUse = null;
		if (beanFactory instanceof DefaultListableBeanFactory) {
			comparatorToUse = ((DefaultListableBeanFactory) beanFactory).getDependencyComparator();
		}
		if (comparatorToUse == null) {
			comparatorToUse = OrderComparator.INSTANCE;
		}
		postProcessors.sort(comparatorToUse);
	}

	/**
	 * Invoke the given BeanDefinitionRegistryPostProcessor beans.
	 */
	private static void invokeBeanDefinitionRegistryPostProcessors(
			Collection<? extends BeanDefinitionRegistryPostProcessor> postProcessors, BeanDefinitionRegistry registry) {

		for (BeanDefinitionRegistryPostProcessor postProcessor : postProcessors) {
			postProcessor.postProcessBeanDefinitionRegistry(registry);
		}
	}

	/**
	 * Invoke the given BeanFactoryPostProcessor beans.
	 */
	private static void invokeBeanFactoryPostProcessors(
			Collection<? extends BeanFactoryPostProcessor> postProcessors, ConfigurableListableBeanFactory beanFactory) {

		for (BeanFactoryPostProcessor postProcessor : postProcessors) {
			postProcessor.postProcessBeanFactory(beanFactory);
		}
	}

	/**
	 * Register the given BeanPostProcessor beans.
	 */
	private static void registerBeanPostProcessors(
			ConfigurableListableBeanFactory beanFactory, List<BeanPostProcessor> postProcessors) {

		for (BeanPostProcessor postProcessor : postProcessors) {
			beanFactory.addBeanPostProcessor(postProcessor);
		}
	}


	/**
	 * BeanPostProcessor that logs an info message when a bean is created during
	 * BeanPostProcessor instantiation, i.e. when a bean is not eligible for
	 * getting processed by all BeanPostProcessors.
	 */
	private static final class BeanPostProcessorChecker implements BeanPostProcessor {

		private static final Log logger = LogFactory.getLog(BeanPostProcessorChecker.class);

		private final ConfigurableListableBeanFactory beanFactory;

		private final int beanPostProcessorTargetCount;

		public BeanPostProcessorChecker(ConfigurableListableBeanFactory beanFactory, int beanPostProcessorTargetCount) {
			this.beanFactory = beanFactory;
			this.beanPostProcessorTargetCount = beanPostProcessorTargetCount;
		}

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName) {
			return bean;
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) {
			if (!(bean instanceof BeanPostProcessor) && !isInfrastructureBean(beanName) &&
					this.beanFactory.getBeanPostProcessorCount() < this.beanPostProcessorTargetCount) {
				if (logger.isInfoEnabled()) {
					logger.info("Bean '" + beanName + "' of type [" + bean.getClass().getName() +
							"] is not eligible for getting processed by all BeanPostProcessors " +
							"(for example: not eligible for auto-proxying)");
				}
			}
			return bean;
		}

		private boolean isInfrastructureBean(@Nullable String beanName) {
			if (beanName != null && this.beanFactory.containsBeanDefinition(beanName)) {
				BeanDefinition bd = this.beanFactory.getBeanDefinition(beanName);
				return (bd.getRole() == RootBeanDefinition.ROLE_INFRASTRUCTURE);
			}
			return false;
		}
	}

}
