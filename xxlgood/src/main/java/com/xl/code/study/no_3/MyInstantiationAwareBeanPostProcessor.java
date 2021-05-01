package com.xl.code.study.no_3;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

/**
 * ClassName: MyInstantiationAwareBeanPostProcessor
 * Description:
 * date: 2020/9/1 10:33
 *
 * @author 小刘讲师，微信：vv517956494
 * 本课程属于 小刘讲师 VIP 源码特训班课程
 * 严禁非法盗用（如有发现非法盗取行为，必将追究法律责任）
 * <p>
 * 如有同学发现非 小刘讲源码 官方号传播本视频资源，请联系我！
 * @since 1.0.0
 */
public class MyInstantiationAwareBeanPostProcessor implements InstantiationAwareBeanPostProcessor {

	/**
	 * InstantiationAwareBeanPostProcessor中自定义的方法
	 * 在方法实例化之前执行  Bean对象还没有
	 */
	@Override
	public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
		System.out.println("--->postProcessBeforeInstantiation");

		// 利用cglib动态代理生成对象返回
		if (beanClass == Student.class) {
			Enhancer e = new Enhancer();
			e.setSuperclass(beanClass);
			e.setCallback(new MethodInterceptor() {
				@Override
				public Object intercept(Object obj, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {

					System.out.println("目标方法执行前:" + method + "\n");
					Object object = methodProxy.invokeSuper(obj, objects);
					System.out.println("目标方法执行后:" + method + "\n");
					return object;
				}
			});
			Student student = (Student) e.create();
			// 返回代理类
			return student;
		}

		return null;
	}
}
