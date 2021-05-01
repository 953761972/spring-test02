package com.xl.code.study.no_7.demo01;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * ClassName: Main
 * Description:
 * date: 2020/11/24 12:07
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
		ApplicationContext applicationContext =
				new ClassPathXmlApplicationContext("spring-aop-test.xml");

		TestInterface test = (TestInterface) applicationContext.getBean("test");
		test.dosomeTest();
		System.out.println("-----");
		test.doOtherTest();
		System.out.println("-----");
		test.a();
		test.b();


	}
}
