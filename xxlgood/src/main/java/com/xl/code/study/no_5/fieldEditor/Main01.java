package com.xl.code.study.no_5.fieldEditor;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * ClassName: Main01
 * Description:
 * date: 2020/10/19 16:45
 *
 * @author 小刘讲师，微信：vv517956494
 * 本课程属于 小刘讲师 VIP 源码特训班课程
 * 严禁非法盗用（如有发现非法盗取行为，必将追究法律责任）
 * <p>
 * 如有同学发现非 小刘讲源码 官方号传播本视频资源，请联系我！
 * @since 1.0.0
 */
public class Main01 {
	public static void main(String[] args) {
		ApplicationContext ac =
				new ClassPathXmlApplicationContext("spring-field-editor01-test.xml");
		Student student = (Student) ac.getBean("student");
		System.out.println(student);
	}
}
