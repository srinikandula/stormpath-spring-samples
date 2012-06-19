package com.stormpath.tooter.controller;

import com.stormpath.sdk.account.Account;
import com.stormpath.sdk.authc.UsernamePasswordRequest;
import com.stormpath.tooter.model.Customer;
import com.stormpath.tooter.model.dao.CustomerDao;
import com.stormpath.tooter.model.sdk.StormpathSDKService;
import com.stormpath.tooter.validator.LoginValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.support.SessionStatus;

import javax.servlet.http.HttpSession;

/**
 * @since 0.1
 */
@Controller
@RequestMapping("/login")
public class LoginController {

    private LoginValidator loginValidator;

    @Autowired
    CustomerDao customerDao;

    @Autowired
    StormpathSDKService stormpathSDKService;

    @Autowired
    public LoginController(LoginValidator loginValidator) {
        this.loginValidator = loginValidator;
    }

    @RequestMapping(method = RequestMethod.POST)
    public String processSubmit(@ModelAttribute("customer") Customer customer,
                                BindingResult result,
                                SessionStatus status,
                                HttpSession session) {

        loginValidator.validate(customer, result);

        String returnStr;

        if (result.hasErrors()) {
            returnStr = "login";
        } else {

            status.setComplete();


            Customer dbCustomer = null;

            try {

                dbCustomer = customerDao.getCustomerByUserName(customer.getUserName());
            } catch (Exception e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }

            if (dbCustomer != null) {

                try {

                    Account account = stormpathSDKService.getApplication().
                            authenticate(
                                    new UsernamePasswordRequest(
                                            dbCustomer.getUserName(),
                                            dbCustomer.getPassword()));

                    session.setAttribute("stormpathAccount", account);

                    session.setAttribute("sessionCustomer", dbCustomer);

                    returnStr = "redirect:/tooter?accountId=" + customer.getUserName();
                } catch (RuntimeException re) {

                    result.addError(new ObjectError("userName", re.getMessage()));
                    re.printStackTrace();
                    returnStr = "login";

                }

            } else {

                result.addError(new ObjectError("userName", "The user with Username '" + customer.getUserName() + "' does not exist."));
                returnStr = "login";
            }

        }

        return returnStr;
    }

    @RequestMapping(method = RequestMethod.GET)
    public String initForm(@ModelAttribute("customer") Customer customer, BindingResult result, ModelMap model) {

        model.addAttribute("customer", customer);

        //return form view
        return "login";
    }

    @RequestMapping(method = RequestMethod.GET, value = "/message")
    public String initMessage(@RequestParam("loginMsg") String messageKey,
                              @ModelAttribute("customer") Customer customer,
                              BindingResult result,
                              ModelMap model) {

        model.addAttribute("customer", customer);
        model.addAttribute("messageKey", messageKey);

        //return form view
        return "login";
    }

}
