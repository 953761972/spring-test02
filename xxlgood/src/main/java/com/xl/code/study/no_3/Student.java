package com.xl.code.study.no_3;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * ClassName: Student
 * Description:
 * date: 2020/9/1 10:20
 *
 * @author 小刘讲师，微信：vv517956494
 * 本课程属于 小刘讲师 VIP 源码特训班课程
 * 严禁非法盗用（如有发现非法盗取行为，必将追究法律责任）
 * <p>
 * 如有同学发现非 小刘讲源码 官方号传播本视频资源，请联系我！
 * @since 1.0.0
 */
public class Student {
	private int id;
	private String name;

	@Autowired
	private Teacher teacher;

	public Student() {
	}

	public Student(int id, String name) {
		this.id = id;
		this.name = name;
	}

	public void start(){
		System.out.println("student bean init method 【start()】 invoked!");
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Teacher getTeacher() {
		return teacher;
	}

	public void setTeacher(Teacher teacher) {
		this.teacher = teacher;
	}


	/*学习...*/
	public void study() {
		System.out.println("因为学习让我们相遇...");
	}
}
