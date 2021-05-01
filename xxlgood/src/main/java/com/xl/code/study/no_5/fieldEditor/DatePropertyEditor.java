package com.xl.code.study.no_5.fieldEditor;

import java.beans.PropertyEditorSupport;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * ClassName: DataPropertyEditor
 * Description:
 * date: 2020/10/19 16:49
 *
 * @author 小刘讲师，微信：vv517956494
 * 本课程属于 小刘讲师 VIP 源码特训班课程
 * 严禁非法盗用（如有发现非法盗取行为，必将追究法律责任）
 * <p>
 * 如有同学发现非 小刘讲源码 官方号传播本视频资源，请联系我！
 * @since 1.0.0
 */
public class DatePropertyEditor extends PropertyEditorSupport {
	private String format = "yyyy-MM-dd";

	public void setFormat(String format) {
		this.format = format;
	}

	@Override
	public void setAsText(String arg0) {
		System.out.println("arg0：" + arg0);
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		try {
			Date d = sdf.parse(arg0);
			this.setValue(d);
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}
}
