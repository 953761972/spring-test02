package com.xl.code.study.no_5.bfpp;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionVisitor;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.util.StringValueResolver;

import java.util.HashSet;
import java.util.Set;

/**
 * ClassName: ReplaceRubbishWordBeanFactoryPostProcessor
 * Description:
 * date: 2020/10/19 17:53
 *
 * @author 小刘讲师，微信：vv517956494
 * 本课程属于 小刘讲师 VIP 源码特训班课程
 * 严禁非法盗用（如有发现非法盗取行为，必将追究法律责任）
 * <p>
 * 如有同学发现非 小刘讲源码 官方号传播本视频资源，请联系我！
 * @since 1.0.0
 */
public class ReplaceObscenitiesBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

	private Set<String> obscenities;


	public ReplaceObscenitiesBeanFactoryPostProcessor() {
		this.obscenities = new HashSet();
	}


	public void setObscenities(Set<String> obscenities) {
		this.obscenities.clear();
		for(String obscenity : obscenities) {
			this.obscenities.add(obscenity);
		}
	}

	public boolean isObscene(Object value) {
		String upperWord = value.toString().toUpperCase();
		return this.obscenities.contains(upperWord);
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		String[] beanNames = beanFactory.getBeanDefinitionNames();
		for(String beanName : beanNames) {
			BeanDefinition bd = beanFactory.getBeanDefinition(beanName);
			StringValueResolver valueResolver = new StringValueResolver() {
				@Override
				public String resolveStringValue(String strVal) {
					if(isObscene(strVal)) {
						return "*****";
					}
					return strVal;
				}
			};
			BeanDefinitionVisitor visitor = new BeanDefinitionVisitor(valueResolver);
			visitor.visitBeanDefinition(bd);
		}
	}
}
