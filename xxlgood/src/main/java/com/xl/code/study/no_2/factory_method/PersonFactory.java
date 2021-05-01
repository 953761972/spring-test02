package com.xl.code.study.no_2.factory_method;

/**
 * ClassName: PersonFactory
 * Description:
 * date: 2020/7/18 18:06
 *
 * @author 小刘讲师，微信：vv517956494
 * 本课程属于 小刘讲师 VIP 源码特训班课程
 * 严禁非法盗用（如有发现非法盗取行为，必将追究法律责任）
 * <p>
 * 如有同学发现非 小刘讲源码 官方号传播本视频资源，请联系我！
 * @since 1.0.0
 */
public class PersonFactory {

	/**
	 * 静态工厂方法
	 */
	public static Person staticBuildPerson(){
		Person person = new Person();
		person.setName("小刘");
		person.setSex(false);
		person.setAge(18);
		return person;
	}

	/**
	 * 实例工厂方法
	 */
	public Person buildPerson() {
		Person person = new Person();
		person.setName("小刘");
		person.setSex(false);
		person.setAge(18);
		return person;
	}
}
