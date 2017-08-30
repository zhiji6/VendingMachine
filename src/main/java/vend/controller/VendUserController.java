package vend.controller;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import base.util.Page;
import vend.entity.VendRole;
import vend.entity.VendUser;
import vend.service.VendRoleService;
import vend.service.VendUserService;

@Controller
@RequestMapping("/user")
public class VendUserController{
	public static Logger logger = Logger.getLogger(VendUserController.class);
	
	@Autowired
	VendUserService vendUserService;
	@Autowired
	VendRoleService vendRoleService;
	/**
	 * 根据输入信息条件查询用户列表，并分页显示
	 * @param model
	 * @param vendUser
	 * @param page
	 * @param request
	 * @return
	 */
	@RequiresPermissions({"user:list"})
	@RequestMapping(value="/users")
	public String listVendUser(Model model,@ModelAttribute VendUser vendUser, @ModelAttribute Page page,HttpServletRequest request) {
		String currentPageStr = request.getParameter("currentPage");
		logger.info(currentPageStr + "===========");
		if(currentPageStr != null){
			int currentPage = Integer.parseInt(currentPageStr);
			page.setCurrentPage(currentPage);
		}
		logger.info(page.toString());
		logger.info(vendUser.toString());
		List<VendUser> vendUsers = vendUserService.listVendUser(vendUser, page);
		model.addAttribute("vendUsers",vendUsers);
		return "manage/user/user_list";
	}
	/**
	 * 跳转用户信息添加界面
	 * @param model
	 * @return
	 */
	@RequiresPermissions({"user:add"})
	@RequestMapping(value="/add",method=RequestMethod.GET)
	public String add(Model model){
		List<VendRole> roles=vendRoleService.findAll();//角色列表
		model.addAttribute("roles",roles);
		model.addAttribute(new VendUser());
		return "manage/user/user_add";
	}
   /**
    * 添加用户信息
    * @param model
    * @param vendUser
    * @param br
    * @return
    */
	@RequiresPermissions({"user:add"})
    @RequestMapping(value="/add",method=RequestMethod.POST)
	public String add(Model model,@Validated VendUser vendUser,BindingResult br){
    	List<VendRole> roles=vendRoleService.findAll();//角色列表
		model.addAttribute("roles",roles);
    	if(br.hasErrors()){
    		return "manage/user/user_add";
    	}
    	vendUserService.insertVendUser(vendUser);
    	return "redirect:users";
	}
    /**
	 * 跳转用户修改界面
	 * @param model
	 * @return
	 */
	@RequiresPermissions({"user:edit"})
	@RequestMapping(value="/{usercode}/edit",method=RequestMethod.GET)
	public String edit(Model model,@PathVariable String usercode){
		List<VendRole> userroles=vendRoleService.findAll();
		model.addAttribute("userroles", userroles);
		VendUser vendUser=vendUserService.getOne(usercode);
		model.addAttribute(vendUser);
		return "manage/user/user_edit";
	}
	/**
	 * 修改用户信息
	 * @param model
	 * @param vendUser
	 * @param br
	 * @return
	 */
	@RequiresPermissions({"user:edit"})
    @RequestMapping(value="/edit",method=RequestMethod.POST)
	public String edit(Model model,@Validated VendUser vendUser,BindingResult br){
    	List<VendRole> userroles=vendRoleService.findAll();
		model.addAttribute("userroles", userroles);
    	if(br.hasErrors()){
    		return "manage/user/user_edit";
    	}
    	int isOk=vendUserService.editVendUser(vendUser);
		return "redirect:users";
	}
    /**
     * 删除用户信息
     * @param user
     * @param br
     * @return
     */
	@RequiresPermissions({"user:del"})
    @RequestMapping(value="/{usercode}/del")
 	public String del(@PathVariable String usercode){
    	vendUserService.delVendUser(usercode);
 		return "redirect:/user/users";
 	}
    /**
     * 批量删除用户信息
     * @param ids
     * @return
     */
	@RequiresPermissions({"user:dels"})
    @RequestMapping(value="/dels")
  	public String dels(HttpServletRequest request){
    	String usercodes=request.getParameter("ids");
    	String usercodeArray[]=usercodes.split(",");
    	int isOk=vendUserService.delVendUsers(usercodeArray);
  		return "redirect:/user/users";
  	}
    /**
     * 从微信小程序接收消费用户信息
     * @param usercode
     * @return
     */
    @RequestMapping(value="/wxuser",method=RequestMethod.GET,produces = "application/json;charset=UTF-8")
    public @ResponseBody VendUser getUserInfo(@PathVariable String nickName){
    	VendUser user=vendUserService.getOne(nickName);
    	if(user==null){
    		//user=new VendUser();
    		//user.setUsercode(nickName);
    		vendUserService.insertVendUser(user);
    	}
    	return user;
    }
}
