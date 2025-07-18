package com.cpt.payments.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.cpt.payments.constants.Constants;
import com.cpt.payments.constants.ErrorCodeEnum;
import com.cpt.payments.exception.TrustlyMockException;
import com.cpt.payments.pojo.request.CoreTrustlyProvider;
import com.cpt.payments.pojo.response.ResponseData;
import com.cpt.payments.util.JsonUtils;
import com.cpt.payments.util.LogMessage;
import com.cpt.payments.util.SHA256RSASignatureVerifier;
import com.cpt.payments.util.SignatureCreator;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class PaymentServiceHelper {

	@Autowired
	private SignatureCreator sigCreator;

	@Autowired
	private SHA256RSASignatureVerifier sigVerify;

	public String prepareSignature(String requestUUID, ResponseData responseData) {
		String signature = null;
		try {
			String serializedData = sigCreator.serializeData(JsonUtils.toJsonNode(responseData));
			String plainText = Constants.METHOD_DEPOSIT + requestUUID + serializedData;
			signature = sigCreator.generateSignature(plainText);
			LogMessage.log(log, "Generating Signature while returning Trusly response::"
					+ "serializedData:" + serializedData + "|plainText:" + plainText + "|signature:" + signature);
		} catch (Exception e) {
			LogMessage.log(log, "Exception processing");
			LogMessage.logException(log, e);

			throw new TrustlyMockException(HttpStatus.INTERNAL_SERVER_ERROR, 
					ErrorCodeEnum.GENERIC_EXCEPTION.getErrorCode(),
					ErrorCodeEnum.GENERIC_EXCEPTION.getErrorMessage(),
					requestUUID,
					Constants.METHOD_DEPOSIT);
		}
		return signature;
	}

	public boolean isSignatureValid(CoreTrustlyProvider trustlyProviderRequest, String requestUUID) {
		String inputSignature = trustlyProviderRequest.getParams().getSignature();

		if (null == inputSignature) {
			LogMessage.log(log, " Input signature NULL for requestUUID:" + requestUUID);
			return false;
		}

		try {
			JsonNode jsonNode = JsonUtils.toJsonNode(trustlyProviderRequest.getParams().getData());
			String plainText = Constants.METHOD_DEPOSIT
					+ "" + trustlyProviderRequest.getParams().getUuid() 
					+ sigCreator.serializeData(jsonNode);

			if (sigVerify.verifySignature(inputSignature, plainText)) {
				LogMessage.log(log, " Signature Valid|inputSignature:" + inputSignature + "|requestUUID:" + requestUUID);
				return true;
			}
		} catch (Exception e) {
			LogMessage.log(log, " Exception while validating signature::requestUUID:" + requestUUID);
			LogMessage.logException(log, e);
		}
		
		return false;
	}

}
