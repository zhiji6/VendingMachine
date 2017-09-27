package vend.controller;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import base.util.Function;
import base.util.HttpClientUtil;
import base.util.MD5Util;
import base.util.Page;
import base.util.SysPara;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import vend.entity.CodeLibrary;
import vend.entity.VendAd;
import vend.entity.VendMachine;
import vend.entity.VendMachineInt;
import vend.entity.VendShopQrcode;
import vend.entity.VendUser;
import vend.service.CodeLibraryService;
import vend.service.VendAdService;
import vend.service.VendMachineIntService;
import vend.service.VendMachineService;
import vend.service.VendParaService;
import vend.service.VendShopQrcodeService;

@Controller
@RequestMapping("/manage")
public class VendManageController{
	public static Logger logger = Logger.getLogger(VendManageController.class);
	
	@Autowired
	VendMachineService vendMachineService;
	@Autowired
	VendMachineIntService vendMachineIntService;
	@Autowired
	VendParaService vendParaService;
	@Autowired
	VendAdService vendAdService;
	@Autowired
	VendShopQrcodeService vendShopQrcodeService;
	@Autowired
	CodeLibraryService codeLibraryService;
	/**
	 * 根据输入信息条件查询机器列表，并分页显示
	 * @param model
	 * @param vendMachine
	 * @param page
	 * @param request
	 * @return
	 */
	@RequiresPermissions({"machine:machines"})
	@RequestMapping(value="/machines")
	public String listVendMachine(Model model,@ModelAttribute VendMachine vendMachine, @ModelAttribute Page page,HttpServletRequest request) {
		String currentPageStr = request.getParameter("currentPage");
		logger.info(currentPageStr + "===========");
		if(currentPageStr != null){
			int currentPage = Integer.parseInt(currentPageStr);
			page.setCurrentPage(currentPage);
		}
		logger.info(page.toString());
		logger.info(vendMachine.toString());
		HttpSession session=request.getSession();
		VendUser user=(VendUser)session.getAttribute("vendUser");
		if(user!=null){
			vendMachine.setUsercode(user.getUsercode());
		}
		List<CodeLibrary> usestatus=codeLibraryService.selectByCodeNo("USESTATUS");
		model.addAttribute("usestatus", usestatus);
		List<VendMachine> vendMachines = vendMachineService.listVendMachine(vendMachine, page);
		model.addAttribute("vendMachines",vendMachines);
		return "manage/machine/machine_list";
	}
	/**
	 * 获取机器信息
	 * @param id
	 * @param response
	 * @return
	 * @throws IOException 
	 */
	@RequestMapping(value="/{id}/getDevList",method=RequestMethod.POST)
	public String getDevList(@PathVariable int id,HttpServletResponse response) throws IOException{
		response.setCharacterEncoding("UTF-8");
		JSONObject json = new JSONObject();
		json.put("success","0");
		json.put("msg", "登陆失败");
		
		VendMachine vendMachine=vendMachineService.getOne(id);
		if(vendMachine==null){
			json.put("success","0");
			json.put("msg", "机器不存在");
			response.getWriter().append(json.toString());
			return null;
		}
		if(vendMachine.getMachineId()==null){
			json.put("success","0");
			json.put("msg", "未绑定机器ID");
			response.getWriter().append(json.toString());
			return null;
		}
		
		Map<String,Object> dataMap = new HashMap<String,Object>();
		dataMap.put("id", vendMachine.getMachineId());
		try {
			String retMsg = HttpClientUtil.httpPostRequest(SysPara.midQueryUrl,dataMap);
			if(StringUtils.isNotBlank(retMsg)){
				JSONObject retJson = JSONObject.fromObject(retMsg);	
				String retCode = retJson.getString("result");
				if(retCode.equals("0")){
					vendMachine.setUseStatus("1");
					int isOk=vendMachineService.editVendMachine(vendMachine);
					if(isOk==1){
						json.put("success","1");
						json.put("msg", "登陆成功");
					}
				}				
			}else{
				System.out.println("登陆失败");
			}
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		response.getWriter().append(json.toString());
		return null; 
	}
	
	/**
	 * 设备登陆
	 * @param id
	 * @param response
	 * @return
	 * @throws IOException 
	 */
	@RequestMapping(value="/{id}/login",method=RequestMethod.POST)
	public String login(@PathVariable int id,HttpServletResponse response) throws IOException{
		response.setCharacterEncoding("UTF-8");
		JSONObject json = new JSONObject();
		json.put("success","0");
		json.put("msg", "登陆失败");
		
		VendMachine vendMachine=vendMachineService.getOne(id);
		if(vendMachine==null){
			json.put("success","0");
			json.put("msg", "机器不存在");
			response.getWriter().append(json.toString());
			return null;
		}
		if(vendMachine.getMachineId()==null){
			json.put("success","0");
			json.put("msg", "未绑定机器ID");
			response.getWriter().append(json.toString());
			return null;
		}
		
		JSONObject payload = new JSONObject();
		payload.accumulate("device_id", vendMachine.getMachineId());
		payload.accumulate("device_type", vendMachine.getMachineType());
		payload.accumulate("operation", "login");
		payload.accumulate("hwAddr", "1234567890abcdef");
		Map<String,Object> dataMap = new HashMap<String,Object>();
		dataMap.put("id", vendMachine.getMachineId());
		dataMap.put("payload", payload);
		try {
			String retMsg = HttpClientUtil.httpPostRequest(SysPara.midPublishUrl,dataMap);
			if(StringUtils.isNotBlank(retMsg)){
				JSONObject retJson = JSONObject.fromObject(retMsg);	
				String retCode = retJson.getString("result");
				if(retCode.equals("0")){
					vendMachine.setUseStatus("1");
					int isOk=vendMachineService.editVendMachine(vendMachine);
					if(isOk==1){
						json.put("success","1");
						json.put("msg", "登陆成功");
					}
				}				
			}else{
				System.out.println("登陆失败");
			}
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		response.getWriter().append(json.toString());
		return null; 
	}
	
	/**
	 * 设备自清洗
	 * @param id
	 * @param response
	 * @return
	 * @throws IOException
	 */
	@RequestMapping(value="/{id}/autoClean",method=RequestMethod.POST)
	public String autoClean(@PathVariable int id,HttpServletResponse response) throws IOException{
		response.setCharacterEncoding("UTF-8");
		JSONObject json = new JSONObject();
		json.put("success","0");
		json.put("msg", "自清洗失败");
		
		VendMachine vendMachine=vendMachineService.getOne(id);
		if(vendMachine==null){
			json.put("success","0");
			json.put("msg", "机器不存在");
			response.getWriter().append(json.toString());
			return null;
		}
		if(vendMachine.getMachineId()==null){
			json.put("success","0");
			json.put("msg", "未绑定机器ID");
			response.getWriter().append(json.toString());
			return null;
		}
		
		JSONObject payload = new JSONObject();
		payload.accumulate("device_id", vendMachine.getMachineId());
		payload.accumulate("operation", "autoClean");
		Map<String,Object> dataMap = new HashMap<String,Object>();
		dataMap.put("id", vendMachine.getMachineId());
		dataMap.put("payload", payload);
		try {
			String retMsg = HttpClientUtil.httpPostRequest(SysPara.midPublishUrl,dataMap);
			if(StringUtils.isNotBlank(retMsg)){
				JSONObject retJson = JSONObject.fromObject(retMsg);	
				String retCode = retJson.getString("error_code");
				if(retCode.equals("0")){
					vendMachine.setCleanStatus("1");;
					int isOk=vendMachineService.editVendMachine(vendMachine);
					if(isOk==1){
						json.put("success","1");
						json.put("msg", "自清洗成功");
					}
				}				
			}else{
				System.out.println("自清洗失败");
			}
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		response.getWriter().append(json.toString());
		return null;   
	}
	
	/**
	 * 设备重启
	 * @param id
	 * @param response
	 * @return
	 * @throws IOException
	 */
	@RequestMapping(value="/{id}/reboot",method=RequestMethod.POST)
	public String reboot(@PathVariable int id,HttpServletResponse response) throws IOException{
		response.setCharacterEncoding("UTF-8");
		JSONObject json = new JSONObject();
		json.put("success","0");
		json.put("msg", "重启失败");

		VendMachine vendMachine=vendMachineService.getOne(id);
		if(vendMachine==null){
			json.put("success","0");
			json.put("msg", "机器不存在");
			response.getWriter().append(json.toString());
			return null;
		}
		if(vendMachine.getMachineId()==null){
			json.put("success","0");
			json.put("msg", "未绑定机器ID");
			response.getWriter().append(json.toString());
			return null;
		}
		
		JSONObject payload = new JSONObject();
		payload.accumulate("device_id", vendMachine.getMachineId());
		payload.accumulate("operation", "reboot");
		Map<String,Object> dataMap = new HashMap<String,Object>();
		dataMap.put("id", vendMachine.getMachineId());
		dataMap.put("payload", payload);
		try {
			String retMsg = HttpClientUtil.httpPostRequest(SysPara.midPublishUrl,dataMap);
			if(StringUtils.isNotBlank(retMsg)){
				JSONObject retJson = JSONObject.fromObject(retMsg);	
				String retCode = retJson.getString("result");
				if(retCode.equals("0")){
					if(vendMachine!=null){
						vendMachine.setUseStatus("1");
						int isOk=vendMachineService.editVendMachine(vendMachine);
						if(isOk==1){
							json.put("success","1");
							json.put("msg", "重启成功");
						}
					}
				}else{
					System.out.println("重启失败:" + retJson.getString("msg"));
				}				
			}else{
				System.out.println("重启失败");
			}
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		response.getWriter().append(json.toString());
		return null;  
	}
	
	/**
	 * 设备关机
	 * @param id
	 * @param response
	 * @return
	 * @throws IOException
	 */
	@RequestMapping(value="/{id}/shutdown",method=RequestMethod.POST)
	public String shutdown(@PathVariable int id,HttpServletResponse response) throws IOException{
		response.setCharacterEncoding("UTF-8");
		JSONObject json = new JSONObject();
		json.put("success","0");
		json.put("msg", "关机失败");

		VendMachine vendMachine=vendMachineService.getOne(id);
		if(vendMachine==null){
			json.put("success","0");
			json.put("msg", "机器不存在");
			response.getWriter().append(json.toString());
			return null;
		}
		if(vendMachine.getMachineId()==null){
			json.put("success","0");
			json.put("msg", "未绑定机器ID");
			response.getWriter().append(json.toString());
			return null;
		}
		
		JSONObject payload = new JSONObject();
		payload.accumulate("device_id", vendMachine.getMachineId());
		payload.accumulate("operation", "shutdown");
		Map<String,Object> dataMap = new HashMap<String,Object>();
		dataMap.put("id", vendMachine.getMachineId());
		dataMap.put("payload", payload);
		try {
			String retMsg = HttpClientUtil.httpPostRequest(SysPara.midPublishUrl,dataMap);
			if(StringUtils.isNotBlank(retMsg)){
				JSONObject retJson = JSONObject.fromObject(retMsg);	
				String retCode = retJson.getString("result");
				if(retCode.equals("0")){
					if(vendMachine!=null){
						vendMachine.setUseStatus("2");
						int isOk=vendMachineService.editVendMachine(vendMachine);
						if(isOk==1){
							json.put("success","1");
							json.put("msg", "关机成功");
						}
					}
				}else{
					System.out.println("关机失败:" + retJson.getString("msg"));
				}				
			}else{
				System.out.println("关机失败");
			}
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		response.getWriter().append(json.toString());
		return null; 
	}
	
	/**
	 * 设备自检
	 * @param id
	 * @param response
	 * @return
	 * @throws IOException
	 */
	@RequestMapping(value="/{id}/selfCheck",method=RequestMethod.POST)
	public String selfCheck(@PathVariable int id,HttpServletResponse response) throws IOException{
		response.setCharacterEncoding("UTF-8");
		JSONObject json = new JSONObject();
		json.put("success","0");
		json.put("msg", "自检失败");

		VendMachine vendMachine=vendMachineService.getOne(id);
		if(vendMachine==null){
			json.put("success","0");
			json.put("msg", "机器不存在");
			response.getWriter().append(json.toString());
			return null;
		}
		if(vendMachine.getMachineId()==null){
			json.put("success","0");
			json.put("msg", "未绑定机器ID");
			response.getWriter().append(json.toString());
			return null;
		}
		
		JSONObject payload = new JSONObject();
		payload.accumulate("device_id", vendMachine.getMachineId());
		payload.accumulate("operation", "selfCheck");
		Map<String,Object> dataMap = new HashMap<String,Object>();
		dataMap.put("id", vendMachine.getMachineId());
		dataMap.put("payload", payload);
		try {
			String retMsg = HttpClientUtil.httpPostRequest(SysPara.midPublishUrl,dataMap);
			if(StringUtils.isNotBlank(retMsg)){
				JSONObject retJson = JSONObject.fromObject(retMsg);	
				String retCode = retJson.getString("result");
				if(retCode.equals("0")){
					json.put("success","1");
					json.put("msg", "自检成功");
				}else{
					System.out.println("自检失败:" + retJson.getString("msg"));
				}				
			}else{
				System.out.println("自检失败");
			}
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		response.getWriter().append(json.toString());
		return null;  
	}
	
	/**
	 * 获取设备参数
	 * @param id
	 * @return
	 */
	@RequestMapping(value="/{id}/getDevParam",method=RequestMethod.POST)
	public String getDevParam(@PathVariable int id,HttpServletResponse response) throws IOException{
		response.setCharacterEncoding("UTF-8");
		JSONObject json = new JSONObject();
		json.put("success","0");
		json.put("msg", "自检失败");

		VendMachine vendMachine=vendMachineService.getOne(id);
		if(vendMachine==null){
			json.put("success","0");
			json.put("msg", "机器不存在");
			response.getWriter().append(json.toString());
			return null;
		}
		if(vendMachine.getMachineId()==null){
			json.put("success","0");
			json.put("msg", "未绑定机器ID");
			response.getWriter().append(json.toString());
			return null;
		}
		
		JSONObject payload = new JSONObject();
		payload.accumulate("device_id", vendMachine.getMachineId());
		payload.accumulate("operation", "getDevParam");
		Map<String,Object> dataMap = new HashMap<String,Object>();
		dataMap.put("id", vendMachine.getMachineId());
		dataMap.put("payload", payload);
		try {
			String retMsg = HttpClientUtil.httpPostRequest(SysPara.midPublishUrl,dataMap);
			if(StringUtils.isNotBlank(retMsg)){
				JSONObject retJson = JSONObject.fromObject(retMsg);	
				String retCode = retJson.getString("result");
				if(retCode.equals("0")){
					json.put("success","1");
					json.put("msg", "获取设备参数成功");
				}
			}else{
				System.out.println("获取设备参数失败");
			}
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		response.getWriter().append(json.toString());
		return null;  
		//String bathPath=vendParaService.selectByParaCode("basePath");
		//return "redirect:"+bathPath+"/machine/machines"; 
	}
	
	/**
	 * 设置设备参数
	 * @param id
	 * @param response
	 * @return
	 * @throws IOException
	 */
	@RequestMapping(value="/{id}/setDevParam",method=RequestMethod.POST)
	public String setDevParam(@PathVariable int id,HttpServletResponse response) throws IOException{
		response.setCharacterEncoding("UTF-8");
		JSONObject json = new JSONObject();
		json.put("success","0");
		json.put("msg", "设置设备参数失败");

		VendMachine vendMachine=vendMachineService.getOne(id);
		if(vendMachine==null){
			json.put("success","0");
			json.put("msg", "机器不存在");
			response.getWriter().append(json.toString());
			return null;
		}
		if(vendMachine.getMachineId()==null){
			json.put("success","0");
			json.put("msg", "未绑定机器ID");
			response.getWriter().append(json.toString());
			return null;
		}
		
		List<VendMachineInt> vendMachineInts=vendMachineIntService.selectByBelongMachine(vendMachine.getId());
		if(vendMachineInts.size()==0){
			json.put("success","0");
			json.put("msg", "请先保存设备参数");
			response.getWriter().append(json.toString());
			return null;
		}
		int isparamset=0;
		for(VendMachineInt vendMachineInt:vendMachineInts){
			if(vendMachineInt!=null){
		    	String hot=vendMachineInt.getHotStatus().equals("0")?"冷":"热";
		    	if(vendMachineInt.getExtend1()!=null&&vendMachineInt.getExtend1().equals("1")){
		    		json.put("success","0");
					json.put("msg", "商品"+vendMachineInt.getGoodsName()+"（"+hot+"）的参数未设置保存");
					isparamset=1;
					break;
		    	}
		    }
		}
		if(isparamset==1){
			response.getWriter().append(json.toString());
			return null;
		}
		
		JSONObject payload = new JSONObject();
		payload.accumulate("device_id", vendMachine.getMachineId());
		payload.accumulate("operation", "setDevParam");
		JSONObject params = new JSONObject();
		params.accumulate("cuptype", "0");
		params.accumulate("hotmax", "80");
		params.accumulate("hotmin", "70");
		params.accumulate("coldmax", "20");
		params.accumulate("coldmin", "20");
		for(int i=0;i<vendMachineInts.size();i++){
			VendMachineInt vendMachineInt1=vendMachineInts.get(i);
			if(vendMachineInt1!=null){
				JSONObject chParam = new JSONObject();
				chParam.accumulate("chNo", (i+1));
				chParam.accumulate("water", vendMachineInt1.getWaterOutTime());
				chParam.accumulate("mater", vendMachineInt1.getGrainOutTime());
				params.accumulate("chParam", chParam);
			}
		}
		payload.accumulate("params", "params");
		Map<String,Object> dataMap = new HashMap<String,Object>();
		dataMap.put("id", vendMachine.getMachineId());
		dataMap.put("payload", payload);
		try {
			String retMsg = HttpClientUtil.httpPostRequest(SysPara.midPublishUrl,dataMap);
			JSONObject retJson = JSONObject.fromObject(retMsg);	
			String retCode = retJson.getString("result");
			if(retCode.equals("0")){
				json.put("success","1");
				json.put("msg", "设置设备参数成功");
			}
			
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		response.getWriter().append(json.toString());
		return null; 
	}
	
	/**
	 * 设置商户二维码
	 * @param id
	 * @param shopQrcode
	 * @param response
	 * @return
	 * @throws IOException
	 */
	@RequestMapping(value="/{id}/{shopQrcode}/setQR",method=RequestMethod.POST)
	public String setQR(@PathVariable int id,@PathVariable int shopQrcode,HttpServletResponse response) throws IOException{
		String bathPath=vendParaService.selectByParaCode("basePath");
		response.setCharacterEncoding("UTF-8");
		JSONObject json = new JSONObject();
		json.put("success","0");
		json.put("msg", "设置商户二维码失败");
		
		VendMachine vendMachine=vendMachineService.getOne(id);
		if(vendMachine==null){
			json.put("success","0");
			json.put("msg", "机器不存在");
			response.getWriter().append(json.toString());
			return null;
		}
		if(vendMachine.getMachineId()==null){
			json.put("success","0");
			json.put("msg", "未绑定机器ID");
			response.getWriter().append(json.toString());
			return null;
		}
		
		VendShopQrcode vendShopQrcode=vendShopQrcodeService.getOne(shopQrcode);//二维码信息
		if(vendShopQrcode==null){
			json.put("success","0");
			json.put("msg", "该二维码信息不存在");
			response.getWriter().append(json.toString());
			return null;
		}
		if(vendShopQrcode.getQrcode()==null){
			json.put("success","0");
			json.put("msg", "您选择的二维码信息未上传二维码图片");
			response.getWriter().append(json.toString());
			return null;
		}
		
		JSONObject payload = new JSONObject();
		payload.accumulate("device_id", vendMachine.getMachineId());
		payload.accumulate("operation", "setQR");
		
		payload.accumulate("qrPic", bathPath+"/"+vendShopQrcode.getQrcode());
		Map<String,Object> dataMap = new HashMap<String,Object>();
		dataMap.put("id", vendMachine.getMachineId());
		dataMap.put("payload", payload);
		try {
			String retMsg = HttpClientUtil.httpPostRequest(SysPara.midPublishUrl,dataMap);
			JSONObject retJson = JSONObject.fromObject(retMsg);	
			String retCode = retJson.getString("result");
			if(retCode.equals("0")){
				json.put("success","1");
				json.put("msg", "设置商户二维码成功");
			}else{
				System.out.println("更新显示的二维码:" + retJson.getString("msg"));
			}	
		} catch (UnsupportedEncodingException e) {
			// TODO 更新显示的二维码异常处理
			e.printStackTrace();
		}
		
		response.getWriter().append(json.toString());
		return null;   
	}
	
	/**
	 * 设置/更新显示的机器识别码
	 * @param id
	 * @param csrCode
	 * @param response
	 * @return
	 * @throws IOException
	 */
	@RequestMapping(value="/{id}/{csrCode}/setCsrCode",method=RequestMethod.POST)
	public String setCsrCode(@PathVariable int id,@PathVariable String csrCode,HttpServletResponse response) throws IOException{
		response.setCharacterEncoding("UTF-8");
		JSONObject json = new JSONObject();
		json.put("success","0");
		json.put("msg", "设置/更新机器识别码失败");
		
		VendMachine vendMachine=vendMachineService.getOne(id);
		if(vendMachine==null){
			json.put("success","0");
			json.put("msg", "机器不存在");
			response.getWriter().append(json.toString());
			return null;
		}
		if(vendMachine.getMachineId()==null){
			json.put("success","0");
			json.put("msg", "未绑定机器ID");
			response.getWriter().append(json.toString());
			return null;
		}
		
		//TODO 机器码需要判断是否重复
		VendMachine vendMachine1=vendMachineService.selectByMachineCode(csrCode);
		if(vendMachine1!=null){
			json.put("success","0");
			json.put("msg", "该机器识别码已被绑定");
			response.getWriter().append(json.toString());
			return null;
		}
		
		JSONObject payload = new JSONObject();
		payload.accumulate("device_id", vendMachine.getMachineId());
		payload.accumulate("operation", "setCsrCode");
		
		payload.accumulate("csrCode", csrCode);
		Map<String,Object> dataMap = new HashMap<String,Object>();
		dataMap.put("id", vendMachine.getMachineId());
		dataMap.put("payload", payload);
		try {
			String retMsg = HttpClientUtil.httpPostRequest(SysPara.midPublishUrl,dataMap);
			JSONObject retJson = JSONObject.fromObject(retMsg);	
			String retCode = retJson.getString("result");
			if(retCode.equals("0")){
				json.put("success","1");
				json.put("msg", "设置/更新机器识别码失败");
			}else{
				System.out.println("设置/更新机器识别码失败:" + retJson.getString("msg"));
			}	
		} catch (UnsupportedEncodingException e) {
			// TODO 更新机器码异常处理
			e.printStackTrace();
		}
		
		response.getWriter().append(json.toString());
		return null;
	}
	
	
	/**
	 * 请求机器节目信息
	 * @param id
	 * @return
	 */
	@RequestMapping(value="/{id}/freshAdItems")
	public String freshAdItems(@PathVariable int id){
		JSONObject payload = new JSONObject();
		payload.accumulate("device_id", id);
		payload.accumulate("operation", "freshAdItems");
		Map<String,Object> dataMap = new HashMap<String,Object>();
		dataMap.put("id", id);
		dataMap.put("payload", payload);
		try {
			String retMsg = HttpClientUtil.httpPostRequest(SysPara.midPublishUrl,dataMap);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String bathPath=vendParaService.selectByParaCode("basePath");
		return "redirect:"+bathPath+"/machine/machines";  
		//String bathPath=vendParaService.selectByParaCode("basePath");
		//return "redirect:"+bathPath+"/machine/machines";  
	}
	
	/**
	 * 设置机器广告节目信息
	 * @param id
	 * @param adId
	 * @param response
	 * @return
	 * @throws IOException
	 */
	@RequestMapping(value="/{id}/{adId}/setAdItemList",method=RequestMethod.POST)
	public String setAdItemList(@PathVariable int id,@PathVariable int adId,HttpServletResponse response) throws IOException{
		String absolutePath=vendParaService.selectByParaCode("absolutePath");//项目绝对路径
		String bathPath=vendParaService.selectByParaCode("basePath");//项目相对路径
		response.setCharacterEncoding("UTF-8");
		JSONObject json = new JSONObject();
		json.put("success","0");
		json.put("msg", "投放广告失败");
		
		VendMachine vendMachine=vendMachineService.getOne(id);
		if(vendMachine==null){
			json.put("success","0");
			json.put("msg", "机器不存在");
			response.getWriter().append(json.toString());
			return null;
		}
		if(vendMachine.getMachineId()==null){
			json.put("success","0");
			json.put("msg", "未绑定机器ID");
			response.getWriter().append(json.toString());
			return null;
		}
		if(vendMachine.getMachineCode()==null){
			json.put("success","0");
			json.put("msg", "未设置机器识别码");
			response.getWriter().append(json.toString());
			return null;
		}
		
		VendAd vendAd=vendAdService.getOne(adId);
		if(vendAd==null){
			json.put("success","0");
			json.put("msg", "该广告不存在");
			response.getWriter().append(json.toString());
			return null;
		}
		if(vendAd.getExtend2()==null){
			json.put("success","0");
			json.put("msg", "该广告未选择广告屏样式");
			response.getWriter().append(json.toString());
			return null;
		}
		if(vendAd.getVideo()==null){
			json.put("success","0");
			json.put("msg", "该广告未选择视频");
			response.getWriter().append(json.toString());
			return null;
		}
		
		JSONObject payload = new JSONObject();
		payload.accumulate("device_id", vendMachine.getMachineId());
		payload.accumulate("operation", "setAdItemList");
		payload.accumulate("qrPic", "1234567890abcdef");
		payload.accumulate("csrCode", vendMachine.getMachineCode());
		payload.accumulate("styleDoc", "http://device.xxx.com/adstyle1.htm");
		
		JSONArray picArray = new JSONArray();
		if(vendAd.getPic1()!=null&&!"".equals(vendAd.getPic1())){
			String filePath=absolutePath+vendAd.getPic1();
			String picName=Function.getPicName(filePath);//图片名
			String picMd5=MD5Util.getMD5(filePath);//图片MD5
			long picSize=Function.getPicSize(filePath);//图片大小
			String picpath=bathPath+filePath;//图片相对路径
			JSONObject pic = new JSONObject();
			pic.accumulate("fileName", picName);
			pic.accumulate("fileMd5", picMd5);
			pic.accumulate("fileSize", picSize);
			pic.accumulate("fileUrl", picpath);
			picArray.add(pic);
		}
		if(vendAd.getPic2()!=null&&!"".equals(vendAd.getPic2())){
			String filePath=absolutePath+vendAd.getPic1();
			String picName=Function.getPicName(filePath);//图片名
			String picMd5=MD5Util.getMD5(filePath);//图片MD5
			long picSize=Function.getPicSize(filePath);//图片大小
			String picpath=bathPath+filePath;//图片相对路径
			JSONObject pic = new JSONObject();
			pic.accumulate("fileName", picName);
			pic.accumulate("fileMd5", picMd5);
			pic.accumulate("fileSize", picSize);
			pic.accumulate("fileUrl", picpath);
			picArray.add(pic);
		}
		if(vendAd.getPic3()!=null&&!"".equals(vendAd.getPic3())){
			String filePath=absolutePath+vendAd.getPic1();
			String picName=Function.getPicName(filePath);//图片名
			String picMd5=MD5Util.getMD5(filePath);//图片MD5
			long picSize=Function.getPicSize(filePath);//图片大小
			String picpath=bathPath+filePath;//图片相对路径
			JSONObject pic = new JSONObject();
			pic.accumulate("fileName", picName);
			pic.accumulate("fileMd5", picMd5);
			pic.accumulate("fileSize", picSize);
			pic.accumulate("fileUrl", picpath);
			picArray.add(pic);
		}
		if(vendAd.getPic4()!=null&&!"".equals(vendAd.getPic4())){
			String filePath=absolutePath+vendAd.getPic1();
			String picName=Function.getPicName(filePath);//图片名
			String picMd5=MD5Util.getMD5(filePath);//图片MD5
			long picSize=Function.getPicSize(filePath);//图片大小
			String picpath=bathPath+filePath;//图片相对路径
			JSONObject pic = new JSONObject();
			pic.accumulate("fileName", picName);
			pic.accumulate("fileMd5", picMd5);
			pic.accumulate("fileSize", picSize);
			pic.accumulate("fileUrl", picpath);
			picArray.add(pic);
		}
		if(vendAd.getPic5()!=null&&!"".equals(vendAd.getPic5())){
			String filePath=absolutePath+vendAd.getPic1();
			String picName=Function.getPicName(filePath);//图片名
			String picMd5=MD5Util.getMD5(filePath);//图片MD5
			long picSize=Function.getPicSize(filePath);//图片大小
			String picpath=bathPath+filePath;//图片相对路径
			JSONObject pic = new JSONObject();
			pic.accumulate("fileName", picName);
			pic.accumulate("fileMd5", picMd5);
			pic.accumulate("fileSize", picSize);
			pic.accumulate("fileUrl", picpath);
			picArray.add(pic);
		}
		if(vendAd.getPic6()!=null&&!"".equals(vendAd.getPic6())){
			String filePath=absolutePath+vendAd.getPic1();
			String picName=Function.getPicName(filePath);//图片名
			String picMd5=MD5Util.getMD5(filePath);//图片MD5
			long picSize=Function.getPicSize(filePath);//图片大小
			String picpath=bathPath+filePath;//图片相对路径
			JSONObject pic = new JSONObject();
			pic.accumulate("fileName", picName);
			pic.accumulate("fileMd5", picMd5);
			pic.accumulate("fileSize", picSize);
			pic.accumulate("fileUrl", picpath);
			picArray.add(pic);
		}
		payload.accumulate("picList", picArray);
		
		if(vendAd.getVideo()!=null){
			String filePath=absolutePath+vendAd.getVideo();
			String videoName=Function.getPicName(filePath);//视频名
			String picMd5=MD5Util.getMD5(filePath);//视频MD5
			long videoSize=Function.getPicSize(filePath);//视频大小
			String videopath=bathPath+filePath;//视频相对路径
			JSONObject mov = new JSONObject();
			JSONArray movArray = new JSONArray();
			mov.accumulate("fileName", videoName);
			mov.accumulate("fileMd5", picMd5);
			mov.accumulate("fileSize", videoSize);
			mov.accumulate("fileUrl", videopath);
			movArray.add(mov);
			payload.accumulate("movList", movArray);
		}
		
		Map<String,Object> dataMap = new HashMap<String,Object>();
		dataMap.put("id", id);
		dataMap.put("payload", payload);
		try {
			String retMsg = HttpClientUtil.httpPostRequest(SysPara.midPublishUrl,dataMap);
			if(StringUtils.isNotBlank(retMsg)){
				JSONObject retJson = JSONObject.fromObject(retMsg);	
				String retCode = retJson.getString("result");
				if(retCode.equals("0")){
					json.put("success","1");
					json.put("msg", "投放广告成功");
				}else{
					System.out.println("投放广告失败:" + retJson.getString("msg"));
				}				
			}else{
				System.out.println("投放广告失败");
			}
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		response.getWriter().append(json.toString());
		return null;  
	}
	
	/**
	 * 上传设备告警信息
	 * @param id
	 * @return
	 */
	@RequestMapping(value="/{id}/reportAlarm")
	public String reportAlarm(@PathVariable int id){
		JSONObject payload = new JSONObject();
		payload.accumulate("device_id", id);
		payload.accumulate("operation", "login");
		payload.accumulate("alarmLv", 1);	
		JSONObject alarms = new JSONObject();
		payload.accumulate("item1", "a1");
		payload.accumulate("item2", "a2");
		payload.accumulate("alarms", alarms);
		Map<String,Object> dataMap = new HashMap<String,Object>();
		dataMap.put("id", id);
		dataMap.put("payload", payload);
		try {
			String retMsg = HttpClientUtil.httpPostRequest(SysPara.midPublishUrl,dataMap);
			if(StringUtils.isNotBlank(retMsg)){
				JSONObject retJson = JSONObject.fromObject(retMsg);	
				String retCode = retJson.getString("result");
				if(retCode.equals("0")){
					/*if(vendMachine!=null){
						vendMachine.setUseStatus("2");
						vendMachineService.editVendMachine(vendMachine);
					}*/
				}else{
					System.out.println("自检失败:" + retJson.getString("msg"));
				}				
			}else{
				System.out.println("自检失败");
			}
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String bathPath=vendParaService.selectByParaCode("basePath");
		return "redirect:"+bathPath+"/machine/machines";  
	}
	
	/**
	 * 售卖指令
	 * @param id
	 * @return
	 */
	@RequestMapping(value="/{id}/sell")
	public String sell(@PathVariable int id){
		JSONObject payload = new JSONObject();
		payload.accumulate("device_id", "1g8p5865c");
		payload.accumulate("operation", "sell");
		payload.accumulate("order", 123456);
		//商品详情
		JSONObject orderGoods = new JSONObject();
		orderGoods.accumulate("chNo", "a1");
		orderGoods.accumulate("count", 1);
		//商品参数详情
		JSONObject params = new JSONObject();
		params.accumulate("selfCup", 1);
		
		orderGoods.accumulate("params", params);
		Map<String,Object> dataMap = new HashMap<String,Object>();
		dataMap.put("id", "1g8p5865c");
		dataMap.put("payload", payload);
		try {
			String retMsg = HttpClientUtil.httpPostRequest(SysPara.midPublishUrl,dataMap);
			if(StringUtils.isNotBlank(retMsg)){
				JSONObject retJson = JSONObject.fromObject(retMsg);	
				String retCode = retJson.getString("errCode");
				if(retCode.equals("0")){
					String device_id = retJson.getString("device_id");
					String operation = retJson.getString("operation");
					String order = retJson.getString("order");
					String msg = retJson.getString("msg");
					System.out.println("售卖失败:" + order + msg);
					/*if(vendMachine!=null){
						vendMachine.setUseStatus("2");
						vendMachineService.editVendMachine(vendMachine);
					}*/
				}else{
					System.out.println("售卖失败:" + retJson.getString("msg"));
				}				
			}else{
				System.out.println("售卖失败");
			}
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String bathPath=vendParaService.selectByParaCode("basePath");
		return "redirect:"+bathPath+"/machine/machines"; 
	}
	
	
	
	/**
	 * 开机
	 * @param id
	 * @return
	 */
	@RequestMapping(value="/{id}/open")
	public String openMachine(@PathVariable int id){
		VendMachine vendMachine=vendMachineService.getOne(id);
		if(vendMachine!=null){
			vendMachine.setUseStatus("1");
			vendMachineService.editVendMachine(vendMachine);
		}
		String bathPath=vendParaService.selectByParaCode("basePath");
		return "redirect:"+bathPath+"/machine/machines"; 
	}
	/**
	 * 机器回调地址
	 * @param map
	 * @return
	 * @throws IOException 
	 */
	@RequestMapping(value="/callback")
	@ResponseBody
	public String callBack(@RequestParam String id,@RequestParam String payload) throws IOException{
		System.out.println(payload);
		logger.info("-------回调结果payload:" + payload);
		System.out.println("-------回调结果payload----------------:" + payload);
		logger.info("-------回调结果id:" + id);
		System.out.println("-------回调结果id----------------:" + id);
		return payload.toString();
	}
	/**
	 * 详情
	 * @param model
	 * @param id
	 * @return
	 */
	@RequestMapping(value="/{id}/detail")
	public String detail(Model model,@PathVariable int id){
		List<VendShopQrcode> clientQrcodes=vendShopQrcodeService.selectByType("2");
		model.addAttribute("clientQrcodes", clientQrcodes);
		VendMachine vendMachine=vendMachineService.getOne(id);
		model.addAttribute("vendMachine", vendMachine);
		return "manage/machine/machine_detail"; 
	}
	/**
	 * 跳转机器信息添加界面
	 * @param model
	 * @return
	 */
	@RequiresPermissions({"machine:add"})
	@RequestMapping(value="/add",method=RequestMethod.GET)
	public String machined(Model model){
		List<CodeLibrary> upvideotypes=codeLibraryService.selectByCodeNo("UPVIDEOTYPE");
		model.addAttribute("upvideotypes", upvideotypes);
		model.addAttribute(new VendMachine());
		return "manage/machine/machine_add";
	}
   /**
    * 添加机器信息
    * @param request
    * @param model
    * @param vendMachine
    * @param br
    * @return
    */
	@RequiresPermissions({"machine:add"})
    @RequestMapping(value="/add",method=RequestMethod.POST)
	public String machined(HttpServletRequest request,Model model,@Validated VendMachine vendMachine,BindingResult br){
		List<CodeLibrary> upvideotypes=codeLibraryService.selectByCodeNo("UPVIDEOTYPE");
		model.addAttribute("upvideotypes", upvideotypes);
    	if(br.hasErrors()){
    		return "manage/machine/machine_add";
    	}
    	vendMachineService.insertVendMachine(vendMachine);
    	return "redirect:machines";
	}
    /**
	 * 跳转机器修改界面
	 * @param model
	 * @return
	 */
	@RequiresPermissions({"machine:edit"})
	@RequestMapping(value="/{id}/edit",method=RequestMethod.GET)
	public String edit(Model model,@PathVariable int id){
		List<VendAd> ads=vendAdService.findAll();
		model.addAttribute("ads", ads);
		List<VendShopQrcode> vendShopQrcodes=vendShopQrcodeService.selectByType("1");
		model.addAttribute("vendShopQrcodes", vendShopQrcodes);
		VendMachine vendMachine=vendMachineService.getOne(id);
		model.addAttribute(vendMachine);
		return "manage/machine/machine_edit";
	}
	/**
	 * 修改机器信息
	 * @param request
	 * @param model
	 * @param vendMachine
	 * @param br
	 * @return
	 */
	@RequiresPermissions({"machine:edit"})
    @RequestMapping(value="/edit",method=RequestMethod.POST)
	public String edit(HttpServletRequest request,Model model,@Validated VendMachine vendMachine,BindingResult br){
    	if(br.hasErrors()){
    		return "manage/machine/machine_edit";
    	}
    	int isOk=vendMachineService.editVendMachine(vendMachine);
		return "redirect:machines";
	}
    /**
     * 删除机器信息
     * @param user
     * @param br
     * @return
     */
	@RequiresPermissions({"machine:del"})
    @RequestMapping(value="/{id}/del")
 	public String del(@PathVariable Integer id){
    	vendMachineService.delVendMachine(id);;
    	String bathPath=vendParaService.selectByParaCode("basePath");
		return "redirect:"+bathPath+"/machine/machines"; 
 	}
    /**
     * 批量删除机器信息
     * @param ids
     * @return
     */
	@RequiresPermissions({"machine:dels"})
    @RequestMapping(value="/dels")
  	public String dels(HttpServletRequest request){
    	String ids=request.getParameter("ids");
    	String idArray[]=ids.split(",");
    	int[] idArray1=new int[idArray.length];
    	for(int i=0;i<idArray.length;i++){
    		idArray1[i]=Function.getInt(idArray[i], 0);
    	}
    	int isOk=vendMachineService.delVendMachines(idArray1);
    	String bathPath=vendParaService.selectByParaCode("basePath");
		return "redirect:"+bathPath+"/machine/machines"; 
  	}
}
