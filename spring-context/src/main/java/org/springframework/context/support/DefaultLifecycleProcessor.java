/*
 * Copyright 2002-2018 the original author or authors.
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.Lifecycle;
import org.springframework.context.LifecycleProcessor;
import org.springframework.context.Phased;
import org.springframework.context.SmartLifecycle;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Default implementation of the {@link LifecycleProcessor} strategy.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @since 3.0
 */
public class DefaultLifecycleProcessor implements LifecycleProcessor, BeanFactoryAware {

	private final Log logger = LogFactory.getLog(getClass());

	private volatile long timeoutPerShutdownPhase = 30000;

	private volatile boolean running;

	@Nullable
	private volatile ConfigurableListableBeanFactory beanFactory;


	/**
	 * Specify the maximum time allotted in milliseconds for the shutdown of
	 * any phase (group of SmartLifecycle beans with the same 'phase' value).
	 * <p>The default value is 30 seconds.
	 */
	public void setTimeoutPerShutdownPhase(long timeoutPerShutdownPhase) {
		this.timeoutPerShutdownPhase = timeoutPerShutdownPhase;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		if (!(beanFactory instanceof ConfigurableListableBeanFactory)) {
			throw new IllegalArgumentException(
					"DefaultLifecycleProcessor requires a ConfigurableListableBeanFactory: " + beanFactory);
		}
		this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
	}

	private ConfigurableListableBeanFactory getBeanFactory() {
		ConfigurableListableBeanFactory beanFactory = this.beanFactory;
		Assert.state(beanFactory != null, "No BeanFactory available");
		return beanFactory;
	}


	// Lifecycle implementation

	/**
	 * Start all registered beans that implement {@link Lifecycle} and are <i>not</i>
	 * already running. Any bean that implements {@link SmartLifecycle} will be
	 * started within its 'phase', and all phases will be ordered from lowest to
	 * highest value. All beans that do not implement {@link SmartLifecycle} will be
	 * started in the default phase 0. A bean declared as a dependency of another bean
	 * will be started before the dependent bean regardless of the declared phase.
	 */
	@Override
	public void start() {
		startBeans(false);
		this.running = true;
	}

	/**
	 * Stop all registered beans that implement {@link Lifecycle} and <i>are</i>
	 * currently running. Any bean that implements {@link SmartLifecycle} will be
	 * stopped within its 'phase', and all phases will be ordered from highest to
	 * lowest value. All beans that do not implement {@link SmartLifecycle} will be
	 * stopped in the default phase 0. A bean declared as dependent on another bean
	 * will be stopped before the dependency bean regardless of the declared phase.
	 */
	@Override
	public void stop() {
		stopBeans();
		this.running = false;
	}

	@Override
	public void onRefresh() {
		startBeans(true);
		this.running = true;
	}

	@Override
	public void onClose() {
		stopBeans();
		this.running = false;
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}


	// Internal helpers

	/**
	 * autoStartupOnly ？
	 * true：表示只启动 SmartLifecycle 生命周期对象，并且启动 SmartLifecycle对象它的autoStartup是true。
	 *       不会启动 普通的 lifecycle 生命周期对象。
	 *
	 * false：全部启动。
	 */
	private void startBeans(boolean autoStartupOnly) {
		// 获取到所有实现了 Lifecycle接口的对象，包装到map内，key是beanName,value 是 lifecycle对象。
		Map<String, Lifecycle> lifecycleBeans = getLifecycleBeans();
		// 因为生命周期对象 可能依赖其它生命周期对象的执行结果，所以需要执行顺序，靠什么实现呢？
		// 靠 phase 数值，phase 越低的 lifecycle 越先执行 start 方法。
		Map<Integer, LifecycleGroup> phases = new HashMap<>();


		lifecycleBeans.forEach((beanName, bean) -> {
			// true：表示只启动 SmartLifecycle 生命周期对象，并且启动 SmartLifecycle对象它的autoStartup是true。
			// false ：全部启动。
			if (!autoStartupOnly || (bean instanceof SmartLifecycle && ((SmartLifecycle) bean).isAutoStartup())) {
				// 获取当前lifecycle对象的 执行排序值。
				int phase = getPhase(bean);

				// LifecycleGroup 内部存储的都是 phase 值一致的 lifecycle。
				LifecycleGroup group = phases.get(phase);
				if (group == null) {
					group = new LifecycleGroup(phase, this.timeoutPerShutdownPhase, lifecycleBeans, autoStartupOnly);
					phases.put(phase, group);
				}
				// 将当前lifecycle添加到 当前phase值一致的 group 内。
				group.add(beanName, bean);
			}
		});

		//执行到这里，lifecycle就按照phase分完组了..

		if (!phases.isEmpty()) {
			List<Integer> keys = new ArrayList<>(phases.keySet());
			//从小到大的一个排序。
			Collections.sort(keys);

			for (Integer key : keys) {
				//执行 lifecycleGroup.start() 启动分组内的lifecycle。
				phases.get(key).start();
			}
		}
	}

	/**
	 * Start the specified bean as part of the given set of Lifecycle beans,
	 * making sure that any beans that it depends on are started first.
	 * @param lifecycleBeans a Map with bean name as key and Lifecycle instance as value
	 * @param beanName the name of the bean to start
	 */
	private void doStart(Map<String, ? extends Lifecycle> lifecycleBeans, String beanName, boolean autoStartupOnly) {
		// 确保lifecycle只被启动一次。 在一个分组内 被启动了，其它分组内，就看不到该lifecycle了。
		Lifecycle bean = lifecycleBeans.remove(beanName);

		if (bean != null && bean != this) {

			// 获取当前即将要被启动的lifecycle所依赖的其它beanName
			String[] dependenciesForBean = getBeanFactory().getDependenciesForBean(beanName);

			// 先启动当前lifecycle所依赖的lifecycle。
			for (String dependency : dependenciesForBean) {
				doStart(lifecycleBeans, dependency, autoStartupOnly);
			}


			if (!bean.isRunning() &&
					// true：表示只启动 SmartLifecycle 生命周期对象，并且启动 SmartLifecycle对象它的autoStartup是true。
					// false ：全部启动。
					(!autoStartupOnly || !(bean instanceof SmartLifecycle) || ((SmartLifecycle) bean).isAutoStartup())) {
				if (logger.isTraceEnabled()) {
					logger.trace("Starting bean '" + beanName + "' of type [" + bean.getClass().getName() + "]");
				}
				try {
					//启动当前lifecycle。
					bean.start();
				}
				catch (Throwable ex) {
					throw new ApplicationContextException("Failed to start bean '" + beanName + "'", ex);
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Successfully started bean '" + beanName + "'");
				}
			}
		}
	}

	private void stopBeans() {
		// 获取到所有实现了 Lifecycle接口的对象，包装到map内，key是beanName,value 是 lifecycle对象。
		Map<String, Lifecycle> lifecycleBeans = getLifecycleBeans();
		// 因为生命周期对象 可能依赖其它生命周期对象的执行结果，所以需要执行顺序，靠什么实现呢？
		// 靠 phase 数值，phase 越低的 lifecycle 越晚执行 stop 方法。
		Map<Integer, LifecycleGroup> phases = new HashMap<>();


		lifecycleBeans.forEach((beanName, bean) -> {
			int shutdownPhase = getPhase(bean);
			LifecycleGroup group = phases.get(shutdownPhase);
			if (group == null) {
				group = new LifecycleGroup(shutdownPhase, this.timeoutPerShutdownPhase, lifecycleBeans, false);
				phases.put(shutdownPhase, group);
			}
			group.add(beanName, bean);
		});


		if (!phases.isEmpty()) {
			List<Integer> keys = new ArrayList<>(phases.keySet());

			//降序 ，lifecycle 最先启动的，最晚关闭；最晚启动，最先关闭。
			keys.sort(Collections.reverseOrder());
			for (Integer key : keys) {
				phases.get(key).stop();
			}
		}
	}

	/**
	 * Stop the specified bean as part of the given set of Lifecycle beans,
	 * making sure that any beans that depends on it are stopped first.
	 * @param lifecycleBeans a Map with bean name as key and Lifecycle instance as value
	 * @param beanName the name of the bean to stop
	 */
	private void doStop(Map<String, ? extends Lifecycle> lifecycleBeans, final String beanName,
			final CountDownLatch latch, final Set<String> countDownBeanNames) {

		Lifecycle bean = lifecycleBeans.remove(beanName);
		if (bean != null) {
			// 依赖当前lifecycle的其它对象 beanName.
			String[] dependentBeans = getBeanFactory().getDependentBeans(beanName);

			// 因为当前lifecycle即将要关闭了，所以那些依赖了 当前lifecycle的bean，如果也是lifecycle对象的话，也要先于当前对象关闭。
			for (String dependentBean : dependentBeans) {
				doStop(lifecycleBeans, dependentBean, latch, countDownBeanNames);
			}

			try {

				if (bean.isRunning()) {

					// 条件成立：当前bean是SmartLifecycle。
					if (bean instanceof SmartLifecycle) {
						if (logger.isTraceEnabled()) {
							logger.trace("Asking bean '" + beanName + "' of type [" +
									bean.getClass().getName() + "] to stop");
						}
						// 将当前SmartLifecycle beanName 添加到 countDownBeanNames 集合内，该集合表示正在关闭的 smartLifecycle。
						countDownBeanNames.add(beanName);

						// Smartlifecycle 可以传递一个Callback ，理论上可以支持异步关闭 生命周期 对象了。
						((SmartLifecycle) bean).stop(() -> {
							latch.countDown();
							countDownBeanNames.remove(beanName);
							if (logger.isDebugEnabled()) {
								logger.debug("Bean '" + beanName + "' completed its stop procedure");
							}
						});
					}
					else {
						// 普通的lifecycle 对象，直接调用 lifecycle.stop() 。
						if (logger.isTraceEnabled()) {
							logger.trace("Stopping bean '" + beanName + "' of type [" +
									bean.getClass().getName() + "]");
						}
						bean.stop();
						if (logger.isDebugEnabled()) {
							logger.debug("Successfully stopped bean '" + beanName + "'");
						}
					}
				}
				else if (bean instanceof SmartLifecycle) {
					// Don't wait for beans that aren't running...
					latch.countDown();
				}
			}
			catch (Throwable ex) {
				if (logger.isWarnEnabled()) {
					logger.warn("Failed to stop bean '" + beanName + "'", ex);
				}
			}
		}
	}


	// overridable hooks

	/**
	 * Retrieve all applicable Lifecycle beans: all singletons that have already been created,
	 * as well as all SmartLifecycle beans (even if they are marked as lazy-init).
	 * @return the Map of applicable beans, with bean names as keys and bean instances as values
	 */
	protected Map<String, Lifecycle> getLifecycleBeans() {
		ConfigurableListableBeanFactory beanFactory = getBeanFactory();
		Map<String, Lifecycle> beans = new LinkedHashMap<>();
		String[] beanNames = beanFactory.getBeanNamesForType(Lifecycle.class, false, false);
		for (String beanName : beanNames) {
			String beanNameToRegister = BeanFactoryUtils.transformedBeanName(beanName);
			boolean isFactoryBean = beanFactory.isFactoryBean(beanNameToRegister);
			String beanNameToCheck = (isFactoryBean ? BeanFactory.FACTORY_BEAN_PREFIX + beanName : beanName);
			if ((beanFactory.containsSingleton(beanNameToRegister) &&
					(!isFactoryBean || matchesBeanType(Lifecycle.class, beanNameToCheck, beanFactory))) ||
					matchesBeanType(SmartLifecycle.class, beanNameToCheck, beanFactory)) {
				Object bean = beanFactory.getBean(beanNameToCheck);
				if (bean != this && bean instanceof Lifecycle) {
					beans.put(beanNameToRegister, (Lifecycle) bean);
				}
			}
		}
		return beans;
	}

	private boolean matchesBeanType(Class<?> targetType, String beanName, BeanFactory beanFactory) {
		Class<?> beanType = beanFactory.getType(beanName);
		return (beanType != null && targetType.isAssignableFrom(beanType));
	}

	/**
	 * Determine the lifecycle phase of the given bean.
	 * <p>The default implementation checks for the {@link Phased} interface, using
	 * a default of 0 otherwise. Can be overridden to apply other/further policies.
	 * @param bean the bean to introspect
	 * @return the phase (an integer value)
	 * @see Phased#getPhase()
	 * @see SmartLifecycle
	 */
	protected int getPhase(Lifecycle bean) {
		return (bean instanceof Phased ? ((Phased) bean).getPhase() : 0);
	}


	/**
	 * Helper class for maintaining a group of Lifecycle beans that should be started
	 * and stopped together based on their 'phase' value (or the default value of 0).
	 */
	private class LifecycleGroup {

		private final int phase;

		private final long timeout;

		private final Map<String, ? extends Lifecycle> lifecycleBeans;

		private final boolean autoStartupOnly;

		private final List<LifecycleGroupMember> members = new ArrayList<>();

		private int smartMemberCount;

		public LifecycleGroup(
				int phase, long timeout, Map<String, ? extends Lifecycle> lifecycleBeans, boolean autoStartupOnly) {

			this.phase = phase;
			this.timeout = timeout;
			this.lifecycleBeans = lifecycleBeans;
			this.autoStartupOnly = autoStartupOnly;
		}

		public void add(String name, Lifecycle bean) {
			this.members.add(new LifecycleGroupMember(name, bean));
			if (bean instanceof SmartLifecycle) {
				this.smartMemberCount++;
			}
		}

		public void start() {
			if (this.members.isEmpty()) {
				return;
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Starting beans in phase " + this.phase);
			}
			//排序..
			Collections.sort(this.members);


			for (LifecycleGroupMember member : this.members) {
				doStart(this.lifecycleBeans, member.name, this.autoStartupOnly);
			}

		}

		public void stop() {
			if (this.members.isEmpty()) {
				return;
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Stopping beans in phase " + this.phase);
			}
			this.members.sort(Collections.reverseOrder());


			// 创建了latch，并且设置latch内部的值为  当前分组内 smartLifecycle的数量。
			CountDownLatch latch = new CountDownLatch(this.smartMemberCount);

			// 保存当前正在处于 关闭ing 的 smartLifecycle beanName。
			Set<String> countDownBeanNames = Collections.synchronizedSet(new LinkedHashSet<>());


			// bf 全部的lifecycle BeanNames
			Set<String> lifecycleBeanNames = new HashSet<>(this.lifecycleBeans.keySet());




			// 处理本分组内的需要关闭的 lifecycle。
			for (LifecycleGroupMember member : this.members) {

				if (lifecycleBeanNames.contains(member.name)) {

					doStop(this.lifecycleBeans, member.name, latch, countDownBeanNames);
				}
				else if (member.bean instanceof SmartLifecycle) {
					// Already removed: must have been a dependent bean from another phase
					latch.countDown();
				}
			}


			try {
				// 关闭分组lifecycle的主线程，会在这里等待，等待latch 归0，或者超时。   30s.
				latch.await(this.timeout, TimeUnit.MILLISECONDS);

				if (latch.getCount() > 0 && !countDownBeanNames.isEmpty() && logger.isInfoEnabled()) {
					logger.info("Failed to shut down " + countDownBeanNames.size() + " bean" +
							(countDownBeanNames.size() > 1 ? "s" : "") + " with phase value " +
							this.phase + " within timeout of " + this.timeout + "ms: " + countDownBeanNames);
				}
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		}
	}


	/**
	 * Adapts the Comparable interface onto the lifecycle phase model.
	 */
	private class LifecycleGroupMember implements Comparable<LifecycleGroupMember> {

		private final String name;

		private final Lifecycle bean;

		LifecycleGroupMember(String name, Lifecycle bean) {
			this.name = name;
			this.bean = bean;
		}

		@Override
		public int compareTo(LifecycleGroupMember other) {
			int thisPhase = getPhase(this.bean);
			int otherPhase = getPhase(other.bean);
			return Integer.compare(thisPhase, otherPhase);
		}
	}

}
