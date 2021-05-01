package com.xl.code.study.no_6.demo01;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;

import java.lang.reflect.Method;

/**
 * ClassName: MyPointcut
 * Description:
 * date: 2020/11/16 12:17
 *
 * @author 小刘讲师，微信：vv517956494
 * 本课程属于 小刘讲师 VIP 源码特训班课程
 * 严禁非法盗用（如有发现非法盗取行为，必将追究法律责任）
 * <p>
 * 如有同学发现非 小刘讲源码 官方号传播本视频资源，请联系我！
 * @since 1.0.0
 */
public class MyPointcut implements Pointcut {
	@Override
	public ClassFilter getClassFilter() {
		return new ClassFilter() {
			@Override
			public boolean matches(Class<?> clazz) {
				return true;
			}
		};
	}

	@Override
	public MethodMatcher getMethodMatcher() {

		return new MethodMatcher() {
			@Override
			public boolean matches(Method method, Class<?> targetClass) {
				if(method.getName().equals("eat")) {
					return true;
				}
				return false;
			}

			@Override
			public boolean isRuntime() {
				return false;
			}

			@Override
			public boolean matches(Method method, Class<?> targetClass, Object... args) {
				return false;
			}
		};
	}
}
