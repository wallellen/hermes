package com.jlfex.hermes.common.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import com.jlfex.hermes.common.App;
import com.jlfex.hermes.common.cache.Caches;
import com.jlfex.hermes.common.exception.ServiceException;

/**
 * 数字工具
 * 
 * @author ultrafrog
 * @version 1.0, 2013-12-30
 * @since 1.0
 */
public abstract class Numbers {

	private static final String PREFIX				= "NUMBER_FORMAT_";
	private static final String PATTERN_CURRENCY	= App.config("app.pattern.currency", "#,##0.00");
	private static final String PATTERN_PERCENT		= App.config("app.pattern.percent", "#0.00%");
	private static final String ROUNDING_CURRENCY 	= App.config("app.number.currency.rounding", "down");
	private static final String ROUNDING_PERCENT	= App.config("app.number.percent.rounding", "down");
	private static final Integer SCALE_CURRENCY		= Integer.valueOf(App.config("app.number.currency.scale", "2"));
	private static final Integer SCALE_PERCENT		= Integer.valueOf(App.config("app.number.percent.scale", "4"));
	
	private static Map<String, RoundingMode> roundingMap;
	
	static {
		roundingMap = new HashMap<String, RoundingMode>();
		roundingMap.put("up", RoundingMode.UP);
		roundingMap.put("down", RoundingMode.DOWN);
		roundingMap.put("ceiling", RoundingMode.CEILING);
		roundingMap.put("floor", RoundingMode.FLOOR);
		roundingMap.put("half_up", RoundingMode.HALF_UP);
		roundingMap.put("half_down", RoundingMode.HALF_DOWN);
		roundingMap.put("half_even", RoundingMode.HALF_EVEN);
	}
	
	/**
	 * 读取数字格式
	 * 
	 * @param pattern
	 * @return
	 */
	public static NumberFormat getNumberFormat(String pattern) {
		NumberFormat format = Caches.get(PREFIX + pattern, NumberFormat.class);
		if (format == null) {
			format = new DecimalFormat(pattern);
			Caches.add(PREFIX + pattern, format);
		}
		return format;
	}
	
	/**
	 * 货币处理
	 * 
	 * @param currency
	 * @return
	 */
	public static BigDecimal currency(Double currency) {
		return currency(BigDecimal.valueOf(currency));
	}
	
	/**
	 * 货币处理
	 * 
	 * @param currency
	 * @return
	 */
	public static BigDecimal currency(BigDecimal currency) {
		return currency.setScale(SCALE_CURRENCY, getRoundingMode(ROUNDING_CURRENCY));
	}
	
	/**
	 * 百分比处理
	 * 
	 * @param percent
	 * @return
	 */
	public static BigDecimal percent(Double percent) {
		return percent(BigDecimal.valueOf(percent));
	}
	
	/**
	 * 百分比处理
	 * 
	 * @param percent
	 * @return
	 */
	public static BigDecimal percent(BigDecimal percent) {
		return percent.setScale(SCALE_PERCENT, getRoundingMode(ROUNDING_PERCENT));
	}
	
	/**
	 * 格式化货币
	 * 
	 * @param currency
	 * @return
	 */
	public static String toCurrency(Double currency) {
		return toCurrency(BigDecimal.valueOf(currency));
	}
	
	/**
	 * 格式化货币
	 * 
	 * @param currency
	 * @return
	 */
	public static String toCurrency(BigDecimal currency) {
		return getNumberFormat(PATTERN_CURRENCY).format(currency(currency));
	}
	
	/**
	 * 格式化百分比
	 * 
	 * @param percent
	 * @return
	 */
	public static String toPercent(Double percent) {
		return toPercent(BigDecimal.valueOf(percent));
	}
	
	/**
	 * 格式化百分比
	 * 
	 * @param percent
	 * @return
	 */
	public static String toPercent(BigDecimal percent) {
		return getNumberFormat(PATTERN_PERCENT).format(percent(percent));
	}
	
	/**
	 * 读取取舍模式
	 * 
	 * @param mode
	 * @return
	 */
	public static RoundingMode getRoundingMode(String mode) {
		if (roundingMap.containsKey(mode)) return roundingMap.get(mode);
		throw new ServiceException("cannot get rounding mode by " + mode + ".");
	}
	
	/**
	 * 解析货币
	 * 
	 * @param pattern
	 * @return
	 */
	public static BigDecimal parseCurrency(String currency) {
		try {
			return BigDecimal.valueOf(getNumberFormat(PATTERN_CURRENCY).parse(currency).doubleValue());
		} catch (ParseException e) {
			throw new ServiceException("cannot parse currency " + currency + ".", e);
		}
	}
	
	/**
	 * 解析百分比
	 * 
	 * @param percent
	 * @return
	 */
	public static BigDecimal parsePercent(String percent) {
		try {
			return BigDecimal.valueOf(getNumberFormat(PATTERN_PERCENT).parse(percent).doubleValue());
		} catch (ParseException e) {
			throw new ServiceException("cannot parse percent " + percent + ".", e);
		}
	}
}
