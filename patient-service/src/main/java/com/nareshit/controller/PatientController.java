package com.nareshit.controller;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import javax.websocket.server.PathParam;

//import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.nareshit.bean.PatientBean;
import com.nareshit.proxy.DoctorServiceProxy;
import com.nareshit.service.PatientService;
import com.nareshit.util.DoctorServiceLocator;
import com.nareshit.util.PropertiesUtil;
import com.nareshit.util.ServiceConstants;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.ribbon.proxy.annotation.Hystrix;

//@Controller
@RestController
@RequestMapping("/patientCntrl")
public class PatientController {
private static final Logger logger = Logger.getLogger(PatientController.class.getName());
	@Autowired
	PropertiesUtil propsUtil;
	
	@Autowired
	private PatientService patService;
	
	@Autowired
	DoctorServiceProxy proxy;
	
	@Autowired
	//RabbitTemplate temp;
	
	RabbitMessagingTemplate temp;
	
	/*@RequestMapping("/getPatDetails")
	public ResponseEntity<PatientBean> getPatientDetailsById(@PathParam("patId")int patId) {
		
		PatientBean pat = patService.getPatientByid(patId);
		return new ResponseEntity<PatientBean>(pat,HttpStatus.OK);
	}*/
	
	@RequestMapping(value="/getPatDetails/{patId}")

	public PatientBean getPatientDetailsById(@PathVariable("patId")int patId) {
		
		PatientBean pat = patService.getPatientByid(patId);
		return pat;
	}
	
	/**
	 * used to save the patient details 
	 * where every registered patient is a 
	 * locked user.
	 * @param pat
	 * @return
	 */
	@HystrixCommand(fallbackMethod="createPatient_fallback")
	@RequestMapping(value="/createPatient",method=RequestMethod.POST)
	public ResponseEntity<String> addPatient(@RequestBody String patJson) {
		String dateFormat = propsUtil.getValueFromKey(ServiceConstants.NOVEL_HEALTH_DATE_FORMAT);
		System.out.println("date format is:\t"+dateFormat);
		
		Gson gson = new Gson();
		PatientBean pat = gson.fromJson(patJson, PatientBean.class);
		
		pat.setCreatedDate(getNovelHealthDateFromat(dateFormat));
		System.out.println("doctor info is:\t"+pat.getDocInfo());
		/*ResponseEntity<String> docResp = proxy.getDoctorByName(pat.getDocInfo());
		pat.setDocInfo(docResp.getBody());*/
		
		pat.setDocInfo(temp.receive("doctorQ").getPayload().toString());
		
		System.out.println("doc name is:\t"+pat.getDocInfo());
		pat = patService.createPatient(pat);
		
		JsonObject json = new JsonObject();
		json.addProperty("status", HttpStatus.CREATED.toString());
		json.addProperty("patDetails", pat.toString());
		return new ResponseEntity<String>(json.toString(),HttpStatus.CREATED); 
	}
	
	
	public ResponseEntity<String> createPatient_fallback(@RequestBody String patJson) {
		String status = "CIRCUIT BREAKER ENABLED!. Service is down at this momemt"+new Date()+
				"please retry after some time";
		return new ResponseEntity<String>(status,HttpStatus.SERVICE_UNAVAILABLE);
	}
	
	
	@RequestMapping(value="/getAllPatients")
	public ResponseEntity<String> getAllPatients(){
		ResponseEntity<String> respEntity = null;
		try {
			List<PatientBean> patBeanList = patService.getAllPatients();
			JSONObject json = new JSONObject();
			json.put("patList",patBeanList);
			json.put("message", "get all patients");
			respEntity = new ResponseEntity<String>(json.toString(), HttpStatus.FOUND);
		}catch (Exception e) {
			JSONObject json = new JSONObject();
			try {
				json.put("error", e.getMessage());
				respEntity = new ResponseEntity<String>(json.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
			} catch (JSONException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		return respEntity;
		
	}
	
	@RequestMapping(value="/getAllPatients/{currpage}/{noOfRecPage}")
	public ResponseEntity<String> getAllPatientsByPaging(@PathVariable("currpage")int currpage,
			@PathVariable("noOfRecPage") int noOfRecPage){
		ResponseEntity<String> respEntity = null;
		try {
			System.out.println("curr page is:\t"+currpage);
			System.out.println("no.OfRecPerPage is:\t"+noOfRecPage);
			List<PatientBean> patBeanList = patService.getAllPatients(currpage,noOfRecPage);
			JSONObject json = new JSONObject();
			json.put("patList",patBeanList);
			json.put("message", "get all patients");
			respEntity = new ResponseEntity<String>(json.toString(), HttpStatus.FOUND);
		}catch (Exception e) {
			JSONObject json = new JSONObject();
			try {
				json.put("error", e.getMessage());
				respEntity = new ResponseEntity<String>(json.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
			} catch (JSONException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		return respEntity;
		
	}
	
	
	@RequestMapping(value="/searchAllPatientsByName/{patName}")
	public ResponseEntity<String> searchPatients(@PathVariable("patName") String patName){
		logger.warning("pat name  in controller is:\t"+patName);
		ResponseEntity<String> respEntity = null;
		try {
			List<PatientBean> patBeanList = patService.SearcgAllPatientsByName(patName);
			JSONObject json = new JSONObject();
			json.put("patList",patBeanList);
			json.put("message", "get all patients");
			respEntity = new ResponseEntity<String>(json.toString(), HttpStatus.FOUND);
		}catch (Exception e) {
			JSONObject json = new JSONObject();
			try {
				json.put("error", e.getMessage());
				respEntity = new ResponseEntity<String>(json.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
			} catch (JSONException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		
		return respEntity;
		
	}
	
	private String getNovelHealthDateFromat(String format) {
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		return sdf.format(new Date());
	}
}
