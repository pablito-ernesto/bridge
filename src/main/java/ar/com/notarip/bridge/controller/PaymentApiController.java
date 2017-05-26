package ar.com.notarip.bridge.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import ar.com.notarip.bridge.PaymentStatus;
import ar.com.notarip.bridge.model.PaymentMP;
import ar.com.notarip.bridge.service.GatewayServiceMP;
import ar.com.notarip.bridge.service.PaymentService;
import ar.com.notarip.bridge.service.dto.PaymentCreateDTO;
import ar.com.notarip.bridge.service.dto.PaymentDTO;
import ar.com.notarip.bridge.util.CustomErrorType;
import ar.com.notarip.bridge.util.RabbitSender;



@RestController
@RequestMapping("/v1/api")
public class PaymentApiController {

	public final static Logger logger = LoggerFactory.getLogger(PaymentApiController.class);

	@Autowired
	PaymentService paymentService;
	
	@Autowired
	GatewayServiceMP gatewayServiceMP;
	
	@Autowired
	RabbitSender sender;

	@RequestMapping(value = "/payment/{id}", method = RequestMethod.GET)
	public ResponseEntity<?> getPayment(@PathVariable("id") String id) {
		logger.info("Fetching payment with id {}", id);

		PaymentDTO payment = paymentService.get(id);

		if (payment == null) {
			String error = buildError("Parent with id {%s} not found .", id);
			logger.error(error);
			return new ResponseEntity<CustomErrorType>(new CustomErrorType(error), HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<PaymentDTO>(payment, HttpStatus.OK);
	}

	@RequestMapping(value = "/payment", method = RequestMethod.POST)
	public ResponseEntity<?> createPayment(@RequestBody PaymentCreateDTO payment, UriComponentsBuilder ucBuilder) {
		logger.info("Creating Payment ", payment);

		PaymentDTO paymentDTO = new PaymentDTO();
		paymentDTO.setCallbackUrl(payment.getCallbackUrl());
		paymentDTO.setCurrency(payment.getCurrency());
		paymentDTO.setDescription(payment.getDescription());
		paymentDTO.setEmail(payment.getEmail());
		paymentDTO.setMount(payment.getMount());
		paymentDTO.setStatus(PaymentStatus.INITIATED);

		String url = paymentService.save(paymentDTO);

		HttpHeaders headers = new HttpHeaders();
		headers.setLocation(ucBuilder.path("/api/payment/{id}").buildAndExpand(paymentDTO.getId()).toUri());
		headers.set("url", url);
		
		
		sender.send("bridge.payment", payment.getCurrency());
		
		return new ResponseEntity<String>(headers, HttpStatus.CREATED);

	}
	
	@RequestMapping(value = "/payment/MPResponse", method = RequestMethod.GET)
	public void notifyPayment(HttpServletRequest request, HttpServletResponse response) {
		
		logger.info("Fetching payment with id {}");

		String collection_status = request.getParameter("collection_status");
		String id = request.getParameter("preference_id");
		
		PaymentMP paymentMP = gatewayServiceMP.getByPreferenceId(id);
		PaymentDTO payment = paymentService.get(paymentMP.getPaymentId());
		String callbackUrl = payment.getCallbackUrl();
		
		if(collection_status.equals(GatewayServiceMP.APPROVED)){
			paymentMP.setStatus(PaymentStatus.APPROVED);
			payment.setStatus(PaymentStatus.APPROVED);
		}
		
		gatewayServiceMP.save(paymentMP);
		
		
		try {
			response.sendRedirect(callbackUrl);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	// @RequestMapping(value = "/comment/{id}", method = RequestMethod.PUT)
	// public ResponseEntity<?> update(@PathVariable("id") String id,
	// @RequestBody CommentUpdateDTO resource) {
	//
	// CommentDTO comment = commentService.get(id);
	//
	// if(comment != null){
	// comment.setText(resource.getText());
	// commentService.update(comment);
	// }else{
	// String error = buildError("Comment with id {%s} not found.", id);
	// logger.error(error);
	// return new ResponseEntity<CustomErrorType>(new CustomErrorType(error),
	// HttpStatus.NOT_FOUND);
	// }
	//
	// HttpHeaders headers = new HttpHeaders();
	// return new ResponseEntity<String>(headers, HttpStatus.OK);
	//
	// }

	// @RequestMapping(value = "/comment/{id}", method = RequestMethod.DELETE)
	// public ResponseEntity<?> deleta(@PathVariable("id") String id) {
	//
	// CommentDTO comment = commentService.get(id);
	//
	// if(comment != null){
	//
	// commentService.delete(comment);
	// }else{
	// String error = buildError("Comment with id {%s} not found.", id);
	// logger.error(error);
	// return new ResponseEntity<CustomErrorType>(new CustomErrorType(error),
	// HttpStatus.NOT_FOUND);
	// }
	//
	// HttpHeaders headers = new HttpHeaders();
	// return new ResponseEntity<String>(headers, HttpStatus.OK);
	//
	// }
	//
	private String buildError(String msg, Object... args) {

		String error = msg;
		error = String.format(error, args);

		return error;
	}

}
