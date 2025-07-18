package com.cpt.payments.exception;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.cpt.payments.constants.ErrorCodeEnum;
import com.cpt.payments.pojo.response.error.ErrorData;
import com.cpt.payments.pojo.response.error.ErrorDetails;
import com.cpt.payments.pojo.response.error.ErrorWrapper;
import com.cpt.payments.pojo.response.error.TrustlyErrorResponse;
import com.cpt.payments.util.JsonUtils;
import com.cpt.payments.util.LogMessage;
import com.cpt.payments.util.SignatureCreator;

import lombok.extern.slf4j.Slf4j;

@ControllerAdvice
@Slf4j
public class TrustlyCoreProviderExceptionHandler {

	@Autowired
	private SignatureCreator creator;
	
	@ExceptionHandler(TrustlyMockException.class)
	public ResponseEntity<TrustlyErrorResponse> handleValidationException(TrustlyMockException ex) {
		LogMessage.log(log, " handleValidationException is -> " + ex.getErrorMessage());
		
		ErrorData errorData = ErrorData.builder()
				.code(ex.getErrorCode())
				.message(ex.getErrorMessage())
				.build();

		String serializedData = creator.serializeData(JsonUtils.toJsonNode(errorData));
		
		String signature = null;
		try {
			String plainText = ex.getMethod() + ex.getUuid() + serializedData;
 			signature = creator.generateSignature(plainText);
 			LogMessage.log(log, " Signature Generated||serializedData" + serializedData +
 					"|plainText:" + plainText + "|signature:" + signature);
		} catch (Exception e) {
			LogMessage.log(log, "Exception processing");
			LogMessage.logException(log, ex);
		}
		
		ErrorDetails errorDetails = ErrorDetails.builder()
				.uuid(ex.getUuid())
				.signature(signature)
				.method(ex.getMethod())
				.data(errorData)
				.build();
		ErrorWrapper errorWrapper = ErrorWrapper.builder()
				.name("JSONRPCError")
				.code(ex.getErrorCode())
				.message(ex.getErrorMessage())
				.error(errorDetails)
				.build();
		TrustlyErrorResponse errorRes = TrustlyErrorResponse.builder()
				.version("1.1")
				.error(errorWrapper)
				.build(); 
		
		LogMessage.log(log, " handleValidationException errorResponse is -> " + errorRes);
		return new ResponseEntity<>(errorRes, ex.getHttpStatus());
	}
	
	
	@ExceptionHandler(Exception.class)
	public ResponseEntity<TrustlyErrorResponse> handleGenericException(Exception ex) {
		LogMessage.log(log, " generic exception message is -> " + ex.getMessage());
		LogMessage.logException(log, ex);
		
		ErrorData errorData = ErrorData.builder()
				.code(ErrorCodeEnum.GENERIC_EXCEPTION.getErrorCode())
				.message(ErrorCodeEnum.GENERIC_EXCEPTION.getErrorMessage())
				.build();
		ErrorDetails errorDetails = ErrorDetails.builder()
				.uuid(null)
				.signature("R9+hjuMqbsH0Ku ... S16VbzRsw==")
				.method("Deposit")
				.data(errorData)
				.build();
		ErrorWrapper errorWrapper = ErrorWrapper.builder()
				.name("JSONRPCError")
				.code(ErrorCodeEnum.GENERIC_EXCEPTION.getErrorCode())
				.message(ErrorCodeEnum.GENERIC_EXCEPTION.getErrorMessage())
				.error(errorDetails)
				.build();
		TrustlyErrorResponse errorRes = TrustlyErrorResponse.builder()
				.version("1.1")
				.error(errorWrapper)
				.build(); 
		
		LogMessage.log(log, " paymentResponse is -> " + errorRes);
		return new ResponseEntity<>(errorRes, HttpStatus.INTERNAL_SERVER_ERROR);
	}
}
