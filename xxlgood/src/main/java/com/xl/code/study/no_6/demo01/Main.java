package com.xl.code.study.no_6.demo01;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.DefaultPointcutAdvisor;

/**
 * ClassName: Main
 * Description:
 * date: 2020/11/16 11:10
 *
 * @author 小刘讲师，微信：vv517956494
 * 本课程属于 小刘讲师 VIP 源码特训班课程
 * 严禁非法盗用（如有发现非法盗取行为，必将追究法律责任）
 * <p>
 * 如有同学发现非 小刘讲源码 官方号传播本视频资源，请联系我！
 * @since 1.0.0
 */
public class Main {
	public static void main(String[] args) {
		//1. 创建被代理对象
		Cat cat = new Cat();

		//2. 创建Spring 代理工厂对象 ProxyFactory
		//   ProxyFactory 是Config + Factory 的存在，持有Aop操作所有的生产资料
		ProxyFactory proxyFactory = new ProxyFactory(cat);

		//3. 添加方法拦截器
		MyPointcut pointcut = new MyPointcut();
		proxyFactory.addAdvisor(new DefaultPointcutAdvisor(pointcut, new MethodInterceptor01()));
		proxyFactory.addAdvisor(new DefaultPointcutAdvisor(pointcut, new MethodInterceptor02()));

		//4. 获取代理对象
		Animal proxy = (Animal) proxyFactory.getProxy();

		proxy.eat();
		System.out.println("--------------------");
		proxy.go();
	}
}
