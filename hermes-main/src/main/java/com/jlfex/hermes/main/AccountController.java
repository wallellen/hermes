package com.jlfex.hermes.main;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSON;
import com.jlfex.hermes.common.App;
import com.jlfex.hermes.common.Logger;
import com.jlfex.hermes.common.Result;
import com.jlfex.hermes.common.cache.Caches;
import com.jlfex.hermes.common.dict.Dicts;
import com.jlfex.hermes.common.exception.ServiceException;
import com.jlfex.hermes.common.utils.Calendars;
import com.jlfex.hermes.common.utils.Numbers;
import com.jlfex.hermes.common.utils.Strings;
import com.jlfex.hermes.common.web.RequestParam;
import com.jlfex.hermes.main.IndexController.HomeNav;
import com.jlfex.hermes.model.BankAccount;
import com.jlfex.hermes.model.Payment;
import com.jlfex.hermes.model.Transaction;
import com.jlfex.hermes.model.User;
import com.jlfex.hermes.model.UserAccount;
import com.jlfex.hermes.model.UserImage;
import com.jlfex.hermes.model.UserProperties;
import com.jlfex.hermes.service.AreaService;
import com.jlfex.hermes.service.BankAccountService;
import com.jlfex.hermes.service.PaymentService;
import com.jlfex.hermes.service.TransactionService;
import com.jlfex.hermes.service.UserInfoService;
import com.jlfex.hermes.service.UserService;
import com.jlfex.hermes.service.web.PropertiesFilter;

/**
 * 账户中心控制器
 * 
 * @author ultrafrog
 * @version 1.0, 2013-12-31
 * @since 1.0
 */
@Controller
@RequestMapping("/account")
public class AccountController {

	private static final String AUTH_TRUE = "1";

	/** 银行账户业务接口 */
	@Autowired
	private BankAccountService bankAccountService;

	/** 用户业务接口 */
	@Autowired
	private UserService userService;

	/** 用户个人信息接口 */
	@Autowired
	private UserInfoService userInfoService;

	/** 交易业务接口 */
	@Autowired
	private TransactionService transactionService;

	/** 支付业务接口 */
	@Autowired
	private PaymentService paymentService;

	/** 地区业务接口 */
	@Autowired
	private AreaService areaService;

	/**
	 * 索引
	 * 
	 * @return
	 */
	@RequestMapping("/index")
	public String index(String type, Model model) {
		App.checkUser();
		model.addAttribute("nav", HomeNav.ACCOUNT);
		model.addAttribute("type", Strings.empty(type, null));
		return "account/index";
	}

	/**
	 * 认证中心
	 * 
	 * @param model
	 * @return
	 */
	@RequestMapping("/approve")
	public String approve(Model model) {
		// 查询用户数据
		App.checkUser();
		UserProperties userProperties = userService.loadPropertiesByUserId(App.user().getId());

		// 设置属性并渲染视图
		model.addAttribute("email", App.user().getAccount());
		model.addAttribute("userProp", userProperties);
		model.addAttribute("idTypes", Dicts.elements(UserProperties.IdType.class));
		model.addAttribute("phoneSwitch", Strings.equals(AUTH_TRUE, App.config("auth.cellphone.switch")));
		model.addAttribute("idSwitch", Strings.equals(AUTH_TRUE, App.config("auth.realname.switch")));
		return "account/approve-new";
	}

	/**
	 * 资金明细
	 * 
	 * @param model
	 * @return
	 */
	@RequestMapping("/detail")
	public String detail(Model model) {
		// 加载数据
		App.checkUser();
		User user = userService.loadById(App.user().getId());
		UserProperties prop = userService.loadPropertiesByUserId(App.user().getId());
		UserAccount cash = userInfoService.loadAccountByUserAndType(user, UserAccount.Type.CASH);
		UserAccount frozen = userInfoService.loadAccountByUserAndType(user, UserAccount.Type.FREEZE);
		String avatar = userService.getAvatar(user, UserImage.Type.AVATAR_LG);

		// 设置参数并渲染视图
		model.addAttribute("beginDate", Calendars.date(Calendars.add(new Date(), Calendar.DATE, -7)));
		model.addAttribute("endDate", Calendars.date());
		model.addAttribute("user", user);
		model.addAttribute("prop", prop);
		model.addAttribute("pass", UserProperties.Auth.PASS);
		model.addAttribute("avatar", avatar);
		model.addAttribute("cash", cash);
		model.addAttribute("frozen", frozen);
		return "account/detail";
	}

	/**
	 * 资金明细数据
	 * 
	 * @param beginDate
	 * @param endDate
	 * @param page
	 * @param size
	 * @param model
	 * @return
	 */
	@RequestMapping("/detail/table")
	public String detailTable(String beginDate, String endDate, Integer page, Integer size, Model model) {
		App.checkUser();
		model.addAttribute("transaction", transactionService.findByUserIdAndDateBetweenAndTypes(App.user().getId(), beginDate, endDate, page, size));
		model.addAttribute("freeze", Transaction.Type.FREEZE);
		model.addAttribute("unfreeze", Transaction.Type.REVERSE_UNFREEZE);
		return "account/detail-table";
	}

	/**
	 * 账户充值
	 * 
	 * @param model
	 * @return
	 */
	@RequestMapping("/charge")
	public String charge(Model model) {
		App.checkUser();
		model.addAttribute("account", userInfoService.loadByUserIdAndType(App.user().getId(), UserAccount.Type.CASH));
		return "account/charge";
	}

	/**
	 * 计算充值手续费
	 * 
	 * @param amount
	 * @return
	 */
	@RequestMapping("/charge/calc")
	@ResponseBody
	public Result<String> calcChargeFee(Double amount) {
		// 计算手续费
		BigDecimal fee = paymentService.calcChargeFee(amount);
		BigDecimal sum = fee.add(Numbers.currency(amount));

		// 设置结果
		Result<String> result = new Result<String>(Result.Type.SUCCESS);
		result.addMessage(Numbers.toCurrency(fee));
		result.addMessage(Numbers.toCurrency(sum));

		// 返回结果
		return result;
	}

	/**
	 * 添加充值
	 * 
	 * @param channel
	 * @param amount
	 * @return
	 */
	@RequestMapping("/charge/add")
	public String addCharge(String channel, Double amount, Model model) {
		App.checkUser();
		Payment payment = paymentService.save(channel, amount);
		//模拟充值
		transactionService.fromCropAccount(Transaction.Type.CHARGE, payment.getUser(), UserAccount.Type.PAYMENT, payment.getAmount(), payment.getId(), App.message("transaction.charge"));
		//扣除充值手续
		transactionService.betweenCropAccount(Transaction.Type.OUT, UserAccount.Type.PAYMENT, UserAccount.Type.PAYMENT_FEE, payment.getFee(), payment.getId(), App.message("transaction.charge.fee"));
		model.addAttribute("payment",payment);
		return "account/charge-success";
	}

	/**
	 * 支付渠道选择
	 * 
	 * @param model
	 * @return
	 */
	@RequestMapping("/channels")
	public String channels(Model model) {
		model.addAttribute("channels", paymentService.findChannelByGroupAll().entrySet());
		return "account/channels";
	}

	/**
	 * 支付返回
	 * 
	 * @param id
	 * @param request
	 * @return
	 */
	@RequestMapping("/payment/return/{id}")
	public String returnPayment(@PathVariable("id") String id, HttpServletRequest request, Model model) {
		model.addAttribute("result", paymentService.callback(id, new RequestParam(request), PaymentService.TYPE_RETURN));
		return "account/payment-return";
	}

	/**
	 * 支付通知
	 * 
	 * @param id
	 * @param request
	 * @param response
	 */
	@RequestMapping("/payment/notify/{id}")
	public void notifyPayment(@PathVariable("id") String id, HttpServletRequest request, HttpServletResponse response) {
		// 处理并获得结果
		Result<Payment> result = paymentService.callback(id, new RequestParam(request), PaymentService.TYPE_NOTIFY);

		// 页面输出
		try {
			response.getWriter().flush();
			response.getWriter().write(result.getFirstMessage());
		} catch (IOException e) {
			throw new ServiceException("cannot write result");
		} finally {
			try {
				response.getWriter().close();
			} catch (IOException e) {
				Logger.error(e.getMessage(), e);
			}
		}
	}

	/**
	 * 提现
	 * 
	 * @return
	 */
	@RequestMapping("/withdraw")
	public String withdraw(Model model) {
		App.checkUser();
		model.addAttribute("bankAccounts", bankAccountService.findEnbaled());
		model.addAttribute("account", userInfoService.loadByUserIdAndType(App.user().getId(), UserAccount.Type.CASH));
		return "account/withdraw";
	}

	/**
	 * 计算提现手续费
	 * 
	 * @param amount
	 * @return
	 */
	@RequestMapping("/withdraw/calc")
	@ResponseBody
	public Result<String> calcWithdrawFee(Double amount) {
		return bankAccountService.calcWithdrawFee(amount);
	}

	/**
	 * 保存提现
	 * 
	 * @param bankAccountId
	 * @param amount
	 * @return
	 */
	@RequestMapping("/withdraw/add")
	public String addWithdraw(String bankAccountId, Double amount, Model model) {
		// 初始化
		App.checkUser();
		Result<String> result = new Result<String>();

		// 保存数据
		try {
			bankAccountService.addWithdraw(bankAccountId, amount);
			result.setType(Result.Type.SUCCESS);
			result.addMessage(App.message("account.fund.withdraw.success"));
		} catch (ServiceException e) {
			result.setType(Result.Type.FAILURE);
			result.setData(e.getCode());
			result.addMessage(App.message(e.getKey()));
			Logger.error(e.getMessage(), e);
		} catch (Exception e) {
			ServiceException se = new ServiceException(e.getMessage(), e);
			result.setType(Result.Type.FAILURE);
			result.setData(se.getCode());
			result.addMessage(App.message(se.getKey()));
			Logger.error(se.getMessage(), se);
		}
		// 渲染视图
		model.addAttribute("result", result);
		return "result-" + result.getTypeName();
	}

	/**
	 * 支付
	 * 
	 * @param id
	 * @param request
	 * @param model
	 * @return
	 */
	@RequestMapping("/payment/{id}")
	public String payment(@PathVariable("id") String id, HttpServletRequest request, Model model) {
		model.addAttribute("form", paymentService.getPaymentForm(id, request.getRemoteAddr()));
		return "account/payment";
	}

	/**
	 * 银行账户表单
	 * 
	 * @param model
	 * @return
	 */
	@RequestMapping("/bank-account/form")
	public String bankAccountForm(Model model) {
		App.checkUser();
		model.addAttribute("banks", bankAccountService.findEnabledBank());
		model.addAttribute("properties", userInfoService.loadPropertiesByUserId(App.user().getId()));
		model.addAttribute("area", JSON.toJSONString(areaService.getAllChildren(null)));
		return "account/bank-account-form";
	}

	/**
	 * 保存银行账单
	 * 
	 * @param bankId
	 * @param cityId
	 * @param deposit
	 * @param account
	 * @return
	 */
	@RequestMapping("/bank-account/save")
	@ResponseBody
	public BankAccount saveBankAccount(String bankId, String cityId, String deposit, String account) {
		App.checkUser();
		return bankAccountService.save(bankId, cityId, deposit, account);
	}

	/**
	 * 清除缓存
	 * 
	 * @param response
	 * @throws IOException
	 */
	@RequestMapping("/clear")
	public void clear(HttpServletResponse response) throws IOException {
		Caches.clear();
		PropertiesFilter.clear();
		response.getWriter().write("success");
	}
}
