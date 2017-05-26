package ar.com.notarip.bridge.service;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.mercadopago.MP;

import ar.com.notarip.bridge.ServiceConfig;
import ar.com.notarip.bridge.model.Payment;
import ar.com.notarip.bridge.repository.PaymentRepository;


@Service(value = "gatewayServiceMP")
public class GatewayServiceMP implements GatewayService {


	@Autowired
	PaymentRepository paymentRepository;
	
	@Autowired
	ServiceConfig serviceConfig;


	@Override
	public String processPayment(Payment payment) {

		String clientid = serviceConfig.getClientId();
		String clientsecret = serviceConfig.getClientSecret();
		String backUrl = serviceConfig.getBackUrl();
		
		paymentRepository.count();
		
		MP mp = new MP (clientid, clientsecret);
		String checkoutURL = null;
		
		JSONObject preference = new JSONObject();
		JSONArray items = new JSONArray();
		JSONObject item = new JSONObject();
		JSONObject payer = new JSONObject();
		JSONObject bac_urls = new JSONObject();
		
		
		
		try {
			
			item.put("title", payment.getDescription());
			item.put("quantity", 1);
			item.put("currency_id", payment.getCurrency());
			item.put("unit_price", payment.getMount());
			items.put(item);
			preference.put("items", items);
			payer.put("email", payment.getEmail());
			preference.put("payer", payer);
			
			bac_urls.put("success", backUrl);
			bac_urls.put("pending", backUrl);
			bac_urls.put("failure", backUrl);
			
			preference.put("back_urls", bac_urls);
			
			preference.put("auto_return", "all");
			
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		try {
			preference = mp.createPreference(preference.toString());
			checkoutURL = preference.getJSONObject("response").getString("init_point");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return checkoutURL;
	}


}
