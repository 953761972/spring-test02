package com.xl.code.study.no_7.demo01;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;

/**
 * ClassName: AspectConfig
 * Description:
 * date: 2020/11/24 11:56
 *
 * @author 小刘讲师，微信：vv517956494
 * 本课程属于 小刘讲师 VIP 源码特训班课程
 * 严禁非法盗用（如有发现非法盗取行为，必将追究法律责任）
 * <p>
 * 如有同学发现非 小刘讲源码 官方号传播本视频资源，请联系我！
 * @since 1.0.0
 */
@Aspect
public class AspectConfig {
	/**
	 * 如何定义一个切点？
	 * 切点位置：com.xl.code 包下的所有class 的所有 test结尾的方法。
	 */
	@Pointcut(value = "execution(* com.xl.code..*.*Test(..))")
	public void test(){}


	/**
	 * 定义一个前置通知
	 */
	@Before(value = "test()")
	public void beforeAdvice() {
		System.out.println("before advice");
	}


	/**
	 * 定义一个后置通知
	 */
	@After(value = "test()")
	public void afterAdvice() {
		System.out.println("after advice");
	}


	/**
	 * 定义一个环绕通知
	 */
	@Around(value = "execution(* com.xl.code..*.*Test(..))")
	public void aroundAdvice(ProceedingJoinPoint joinPoint) {
		try {
			System.out.println("around advice begin");
			joinPoint.proceed();
			System.out.println("around advice end");
		} catch (Throwable throwable) {
			throwable.printStackTrace();
		}
	}



}
