package com.xl.code.study.no_5.multicaster;

import org.springframework.context.ApplicationEvent;

/**
 * ClassName: TestEvent
 * Description:
 * date: 2020/10/20 9:05
 *
 * @author 小刘讲师，微信：vv517956494
 * 本课程属于 小刘讲师 VIP 源码特训班课程
 * 严禁非法盗用（如有发现非法盗取行为，必将追究法律责任）
 * <p>
 * 如有同学发现非 小刘讲源码 官方号传播本视频资源，请联系我！
 * @since 1.0.0
 */
public class TestEvent extends ApplicationEvent {
	public String msg;

	public TestEvent(Object source) {
		super(source);
	}

	public TestEvent(Object source, String msg) {
		super(source);
		this.msg = msg;
	}

	public void print() {
		System.out.println(this.msg);
	}
}
