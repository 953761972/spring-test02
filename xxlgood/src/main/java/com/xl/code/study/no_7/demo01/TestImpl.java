package com.xl.code.study.no_7.demo01;

/**
 * ClassName: TestImpl
 * Description:
 * date: 2020/11/24 12:04
 *
 * @author 小刘讲师，微信：vv517956494
 * 本课程属于 小刘讲师 VIP 源码特训班课程
 * 严禁非法盗用（如有发现非法盗取行为，必将追究法律责任）
 * <p>
 * 如有同学发现非 小刘讲源码 官方号传播本视频资源，请联系我！
 * @since 1.0.0
 */
public class TestImpl implements TestInterface {
	@Override
	public void dosomeTest() {
		System.out.println("dosomeTest execute");
	}

	@Override
	public void doOtherTest() {
		System.out.println("doOtherTest execute");
	}

	@Override
	public void a() {
		System.out.println("a function execute");
	}

	@Override
	public void b() {
		System.out.println("b function execute");
	}
}
