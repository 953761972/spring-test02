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
import org.springframework.beans.factory.support.AbstractBeanFactory;
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


	public static void invokeBeanFactoryPostProcessors(
			ConfigurableListableBeanFactory beanFactory, List<BeanFactoryPostProcessor> beanFactoryPostProcessors) {

		// Invoke BeanDefinitionRegistryPostProcessors first, if any.
		// 存储已经执行过的bfpp beanName
		Set<String> processedBeans = new HashSet<>();

		// 条件成立：说明当前bf是bd注册中心。bd全部注册到bf内。
		if (beanFactory instanceof BeanDefinitionRegistry) {
			// 将bf 转换成 bdRegistry。
			BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;

			// 规整的 普通的 PostProcessor集合。
			List<BeanFactoryPostProcessor> regularPostProcessors = new ArrayList<>();
			// 存储 BeanDefinitionRegistryPostProcessor 集合。
			// BeanDefinitionRegistryPostProcessor 这种类型的容器后处理器 可以再向容器内 手动硬编码注册一些bd。
			List<BeanDefinitionRegistryPostProcessor> registryProcessors = new ArrayList<>();


			// 处理ApplicationContext上面 硬编码注册的一些 bfpp 处理器。
			for (BeanFactoryPostProcessor postProcessor : beanFactoryPostProcessors) {
				if (postProcessor instanceof BeanDefinitionRegistryPostProcessor) {
					BeanDefinitionRegistryPostProcessor registryProcessor =
							(BeanDefinitionRegistryPostProcessor) postProcessor;
					// 这里调用 可以向 bf 内部再次注册一些 bd信息。
					registryProcessor.postProcessBeanDefinitionRegistry(registry);
					// 添加到 集合。
					registryProcessors.add(registryProcessor);
				}
				else {
					//因为当前postProcessor非registry类型，即当前postProcessor是个普通的。就加入到普通集合，后续会统一执行一些方法。
					regularPostProcessors.add(postProcessor);
				}
			}


			// Do not initialize FactoryBeans here: We need to leave all regular beans
			// uninitialized to let the bean factory post-processors apply to them!
			// Separate between BeanDefinitionRegistryPostProcessors that implement
			// PriorityOrdered, Ordered, and the rest.
			// 临时的，当前阶段的 registry后处理器集合。
			List<BeanDefinitionRegistryPostProcessor> currentRegistryProcessors = new ArrayList<>();

			// First, invoke the BeanDefinitionRegistryPostProcessors that implement PriorityOrdered.
			//使用bf获取BeanDefinitionRegistryPostProcessor类型的全部beanName
			String[] postProcessorNames =
					beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);

			//处理每个beanName，每个beanName其实都是BeanDefinitionRegistryPostProcessor。
			for (String ppName : postProcessorNames) {
				// 判断对应的bean是否实现了 主排序接口PriorityOrdered ，如果实现了，就让该 bdrpp 添加到 currentRegistryProcessors。
				if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					// 因为接下来马上就是要执行 bfrpp 的 registry 相关接口方法，所以将beanName添加到processedBeans 表示已执行。
					processedBeans.add(ppName);
				}
			}
			//排序，根据order值进行 升序排序。
			sortPostProcessors(currentRegistryProcessors, beanFactory);
			//添加到registry类型后处理器集合内。
			registryProcessors.addAll(currentRegistryProcessors);
			// 调用每个bdrpp的postProcessBeanDefinitionRegistry 方法，向bf内注册bd信息。
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
			// 清空集合。
			currentRegistryProcessors.clear();



			// Next, invoke the BeanDefinitionRegistryPostProcessors that implement Ordered.
			postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
			for (String ppName : postProcessorNames) {
				//!processedBeans.contains(ppName)  确保每个bdrpp只执行一次 注册 bd的逻辑。
				if (!processedBeans.contains(ppName) && beanFactory.isTypeMatch(ppName, Ordered.class)) {

					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					processedBeans.add(ppName);
				}
			}

			sortPostProcessors(currentRegistryProcessors, beanFactory);
			registryProcessors.addAll(currentRegistryProcessors);
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
			currentRegistryProcessors.clear();


			// Finally, invoke all other BeanDefinitionRegistryPostProcessors until no further ones appear.
			// 控制while是否需要再次循环，因为循环内就是查找并执行bdrpp后处理器的registry相关的接口方法。
			// 接口方法执行之后 会向bf 内注册 bd，再次注册的bd 也有可能是 bdrpp 类型，所以，需要该变量控制while循环。
			boolean reiterate = true;

			while (reiterate) {
				reiterate = false;
				postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
				for (String ppName : postProcessorNames) {
					if (!processedBeans.contains(ppName)) {
						currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
						processedBeans.add(ppName);
						reiterate = true;
					}
				}
				//排序，根据order值进行 升序排序。
				sortPostProcessors(currentRegistryProcessors, beanFactory);
				//添加到registry类型后处理器集合内。
				registryProcessors.addAll(currentRegistryProcessors);
				// 调用每个bdrpp的postProcessBeanDefinitionRegistry 方法，向bf内注册bd信息。
				invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
				//清空当前阶段的集合。
				currentRegistryProcessors.clear();
			}




			// Now, invoke the postProcessBeanFactory callback of all processors handled so far.
			// 因为registry类型的后处理器 继承自 BeanFactoryPostProcessor，bfpp 类型后处理器 有  postProcessBeanFactory()
			// 接口方法需要执行，所以，registry 类型的所有后处理器 不仅要执行 postProcessBeanDefinitionRegistry() 还要执行 postProcessBeanFactory()
			invokeBeanFactoryPostProcessors(registryProcessors, beanFactory);

			// 执行硬编码提供的【beanFactoryPostProcessor】接口实现类的  postProcessBeanFactory() 方法。
			invokeBeanFactoryPostProcessors(regularPostProcessors, beanFactory);
		}

		else {
			// Invoke factory processors registered with the context instance.
			invokeBeanFactoryPostProcessors(beanFactoryPostProcessors, beanFactory);
		}


		//上面的代码处理了：硬编码提供的bfpp 和 bdrpp 这两种类型的bfpp。
		//还有普通的bfpp需要处理。

		// Do not initialize FactoryBeans here: We need to leave all regular beans
		// uninitialized to let the bean factory post-processors apply to them!
		// 获取容器内注册的bfpp类型的 beanName 数组集合。
		String[] postProcessorNames =
				beanFactory.getBeanNamesForType(BeanFactoryPostProcessor.class, true, false);

		// Separate between BeanFactoryPostProcessors that implement PriorityOrdered,
		// Ordered, and the rest.

		//主排序后处理器集合
		List<BeanFactoryPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();

		//次排序后处理器集合
		List<String> orderedPostProcessorNames = new ArrayList<>();

		//未实现排序接口的 后处理器集合
		List<String> nonOrderedPostProcessorNames = new ArrayList<>();

		//筛选出来 未执行的 bfpp，然后根据 bfpp 是否实现 排序接口，来分类到 上面的 3个集合内。
		for (String ppName : postProcessorNames) {
			if (processedBeans.contains(ppName)) {
				// skip - already processed in first phase above
			}

			else if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
				priorityOrderedPostProcessors.add(beanFactory.getBean(ppName, BeanFactoryPostProcessor.class));
			}
			else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
				orderedPostProcessorNames.add(ppName);
			}
			else {
				nonOrderedPostProcessorNames.add(ppName);
			}
		}


		// First, invoke the BeanFactoryPostProcessors that implement PriorityOrdered.
		sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
		invokeBeanFactoryPostProcessors(priorityOrderedPostProcessors, beanFactory);





		// Next, invoke the BeanFactoryPostProcessors that implement Ordered.
		List<BeanFactoryPostProcessor> orderedPostProcessors = new ArrayList<>(orderedPostProcessorNames.size());
		for (String postProcessorName : orderedPostProcessorNames) {
			orderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}

		sortPostProcessors(orderedPostProcessors, beanFactory);
		invokeBeanFactoryPostProcessors(orderedPostProcessors, beanFactory);





		// Finally, invoke all other BeanFactoryPostProcessors.
		List<BeanFactoryPostProcessor> nonOrderedPostProcessors = new ArrayList<>(nonOrderedPostProcessorNames.size());
		for (String postProcessorName : nonOrderedPostProcessorNames) {
			nonOrderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		invokeBeanFactoryPostProcessors(nonOrderedPostProcessors, beanFactory);


		// Clear cached merged bean definitions since the post-processors might have
		// modified the original metadata, e.g. replacing placeholders in values...
		beanFactory.clearMetadataCache();
	}

	public static void registerBeanPostProcessors(
			ConfigurableListableBeanFactory beanFactory, AbstractApplicationContext applicationContext) {
		// 获取后处理器beanName数组。
		String[] postProcessorNames = beanFactory.getBeanNamesForType(BeanPostProcessor.class, true, false);

		// Register BeanPostProcessorChecker that logs an info message when
		// a bean is created during BeanPostProcessor instantiation, i.e. when
		// a bean is not eligible for getting processed by all BeanPostProcessors.
		// 后处理器数量
		int beanProcessorTargetCount = beanFactory.getBeanPostProcessorCount() + 1 + postProcessorNames.length;
		// 检查创建bean实例时，后处理器框架是否已经全部注册完毕，如果未完毕，提示。
		beanFactory.addBeanPostProcessor(new BeanPostProcessorChecker(beanFactory, beanProcessorTargetCount));

		// Separate between BeanPostProcessors that implement PriorityOrdered,
		// Ordered, and the rest.

		// 存储实现了主排序接口的后处理器集合。
		List<BeanPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
		// 存储实现了 MergedBeanDefinitionPostProcessor 类型的后处理器集合。
		List<BeanPostProcessor> internalPostProcessors = new ArrayList<>();
		// 存储实现了 次排序 接口的后处理器name集合。
		List<String> orderedPostProcessorNames = new ArrayList<>();
		// 存储未实现 排序接口的 后处理器name集合。
		List<String> nonOrderedPostProcessorNames = new ArrayList<>();

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
		// 首先注册实现了主排序接口的 后处理器。
		sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
		// 注册。 内部bf.addBeanPostProcessors 确保了不会重复添加 后处理器。
		registerBeanPostProcessors(beanFactory, priorityOrderedPostProcessors);


		// Next, register the BeanPostProcessors that implement Ordered.
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
		// 排序内部处理器。
		sortPostProcessors(internalPostProcessors, beanFactory);
		// 再次注册到bf内，确保MergedBeanDefinitionPostProcessor类型的后处理器，在后处理器List的末尾。
		registerBeanPostProcessors(beanFactory, internalPostProcessors);

		// Re-register post-processor for detecting inner beans as ApplicationListeners,
		// moving it to the end of the processor chain (for picking up proxies etc).
		// 重新注册 ApplicationListenerDetector 后处理器。
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

		if (beanFactory instanceof AbstractBeanFactory) {
			// Bulk addition is more efficient against our CopyOnWriteArrayList there
			((AbstractBeanFactory) beanFactory).addBeanPostProcessors(postProcessors);
		}
		else {
			for (BeanPostProcessor postProcessor : postProcessors) {
				beanFactory.addBeanPostProcessor(postProcessor);
			}
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
			//条件一：成立说明 当前bean类型是普通bean 不是 后处理器类型。
			//条件二：成立说明，当前beanName是用户级别的bean 非spring框架自身的bean。
			//条件三：成立说明，bf上面注册的后处理器数量 < 后处理器总数，说明当前bean创建阶段时，后处理器框架尚未初始化完毕...需要打印日志 提示。
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
