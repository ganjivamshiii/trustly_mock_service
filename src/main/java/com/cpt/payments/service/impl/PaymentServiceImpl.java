package com.cpt.payments.service.impl;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.cpt.payments.constants.Constants;
import com.cpt.payments.constants.ErrorCodeEnum;
import com.cpt.payments.exception.TrustlyMockException;
import com.cpt.payments.http.HttpRequest;
import com.cpt.payments.http.HttpRestTemplateEngine;
import com.cpt.payments.pojo.request.CoreTrustlyProvider;
import com.cpt.payments.pojo.request.TrustlyNotificationRequest;
import com.cpt.payments.pojo.response.ResponseData;
import com.cpt.payments.pojo.response.Result;
import com.cpt.payments.pojo.response.TrustlyCoreResponse;
import com.cpt.payments.service.PaymentService;
import com.cpt.payments.util.LogMessage;
import com.cpt.payments.util.SHA256RSASignatureVerifier;
import com.cpt.payments.util.SignatureCreator;
import com.google.gson.Gson;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class PaymentServiceImpl implements PaymentService {

	private static final String SUCCESS = "SUCCESS";

	@Value("${trustly.initiate.payment.url}")
	private String initiatePaymentUrl;

	@Value("${cpt.notification.url}")
	private String notificationPaymentUrl;

	@Autowired
	private SignatureCreator signatureCreator;

	@Autowired
	private HttpRestTemplateEngine httpRestTemplateEngine;

	@Autowired
	private SHA256RSASignatureVerifier sigVerify;

	@Autowired
	private SignatureCreator sigCreator;

	@Autowired
	PaymentServiceHelper serviceHelper;

	@Override
	public TrustlyCoreResponse initiatePayment(CoreTrustlyProvider trustlyProviderRequest) {
		String requestUUID = trustlyProviderRequest.getParams().getUuid();

		boolean isSigValid = true;// TODO this is only for initial testing
		// boolean isSigValid = serviceHelper.isSignatureValid(trustlyProviderRequest, requestUUID);

		if(!isSigValid) {
			LogMessage.log(log, " INVALID signature for requestUUID:" + requestUUID);
			throw new TrustlyMockException(HttpStatus.UNAUTHORIZED, 
					ErrorCodeEnum.ERROR_UNABLE_TO_VERIFY_RSA_SIGNATURE.getErrorCode(),
					ErrorCodeEnum.ERROR_UNABLE_TO_VERIFY_RSA_SIGNATURE.getErrorMessage(),
					requestUUID,
					Constants.METHOD_DEPOSIT);
		}
		
		if (trustlyProviderRequest.getParams().getData().getUsername() == null
				|| trustlyProviderRequest.getParams().getData().getUsername().isEmpty()) {
			LogMessage.log(log, " MISSING username in requestUUID:" + requestUUID);
			throw new TrustlyMockException(
					HttpStatus.BAD_REQUEST, 
					ErrorCodeEnum.ERROR_USERNAME_MISSING.getErrorCode(),
					ErrorCodeEnum.ERROR_USERNAME_MISSING.getErrorMessage(), 
					requestUUID, 
					Constants.METHOD_DEPOSIT);
		}

		LogMessage.debug(log, " SIGNATURE VALID, continuing request processing");

		String paymentId = UUID.randomUUID().toString();

		ResponseData responseData = ResponseData.builder()
				.orderid(paymentId)
				.url(initiatePaymentUrl + paymentId)
				.build();


		String signature = serviceHelper.prepareSignature(requestUUID, responseData);

		Result result = Result.builder()
				.data(responseData)
				.method("Deposit")
				.uuid(requestUUID)
				.signature(signature)
				.build();

		TrustlyCoreResponse trustlyCoreResponse = TrustlyCoreResponse.builder().result(result).version("1.1").build();
		LogMessage.log(log, " TrustlyCoreResponse is -> " + trustlyCoreResponse);
		return trustlyCoreResponse;
	}

	@Override
	public void processPayment(String paymentId, String status) {

		TrustlyNotificationRequest trustlyNotificationRequest = TrustlyNotificationRequest.builder()
				.paymentId(paymentId).status(status).build();
		if (SUCCESS.equalsIgnoreCase(status)) {
			trustlyNotificationRequest.setCode("AC001");
			trustlyNotificationRequest.setMessage("Transaction succeded");
		} else {
			trustlyNotificationRequest.setCode("FL001");
			trustlyNotificationRequest.setMessage("Transaction Failed");
		}
		LogMessage.log(log, " trustlyNotificationRequest is -> " + trustlyNotificationRequest);

		Gson gson = new Gson();
		try {
			String signature = signatureCreator.generateSignature(gson.toJson(trustlyNotificationRequest));
			HttpHeaders headers = new HttpHeaders();
			headers.add("signature", signature);

			HttpRequest httpRequest = HttpRequest.builder().headers(headers)
					.request(gson.toJson(trustlyNotificationRequest)).httpMethod(HttpMethod.POST)
					.url(notificationPaymentUrl).build();

			httpRestTemplateEngine.execute(httpRequest);

		} catch (Exception e) {
			LogMessage.log(log, "EXception while creating signature ");
		}

	}

}
