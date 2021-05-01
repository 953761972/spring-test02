package com.xl.code.study.no_6.demo01;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * ClassName: MethodInterceptor01
 * Description:
 * date: 2020/11/16 11:13
 *
 * @author 小刘讲师，微信：vv517956494
 * 本课程属于 小刘讲师 VIP 源码特训班课程
 * 严禁非法盗用（如有发现非法盗取行为，必将追究法律责任）
 * <p>
 * 如有同学发现非 小刘讲源码 官方号传播本视频资源，请联系我！
 * @since 1.0.0
 */
public class MethodInterceptor01 implements MethodInterceptor {
	@Nullable
	@Override
	public Object invoke(@Nonnull MethodInvocation invocation) throws Throwable {
		System.out.println("methodInterceptor01 begin");
		Object ret = invocation.proceed();
		System.out.println("methodInterceptor01 end");
		return ret;
	}
}
