package com.xl.code.study.no_2.start;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;

/**
 * ClassName: BeanFactoryTest
 * Description:
 * date: 2020/7/18 10:01
 *
 * @author 小刘讲师，微信：vv517956494
 * 本课程属于 小刘讲师 VIP 源码特训班课程
 * 严禁非法盗用（如有发现非法盗取行为，必将追究法律责任）
 * <p>
 * 如有同学发现非 小刘讲源码 官方号传播本视频资源，请联系我！
 * @since 1.0.0
 */
public class BeanFactoryTest {
	public static void main(String[] args) {
		BeanFactory beanFactory = new XmlBeanFactory(new ClassPathResource("spring-bf.xml"));
		Object a = beanFactory.getBean("componentA");
		Object b = beanFactory.getBean("componentB");

		System.out.println(a);
		System.out.println(b);

	}
}
