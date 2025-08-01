package com.cpt.payments.http;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import com.cpt.payments.util.LogMessage;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class HttpRestTemplateEngine {

	public ResponseEntity<String> execute(HttpRequest httpRequest) {
		try {
			RestTemplate restTemplate = new RestTemplate();
			restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
			headers.addAll(httpRequest.getHeaders());
			HttpEntity<?> request = new HttpEntity<>(httpRequest.getRequest(), headers);

			SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
			requestFactory.setOutputStreaming(false);
			restTemplate.setRequestFactory(new BufferingClientHttpRequestFactory(requestFactory));

			HttpMethod method = prepareHttpMethod(httpRequest.getHttpMethod());

			ResponseEntity<String> response = restTemplate.exchange(httpRequest.getUrl(), method, request, String.class);

			HttpStatusCode statusCode = response.getStatusCode();

			LogMessage.debug(log, "Got API response with statusCode:" + statusCode);

			if (statusCode.is2xxSuccessful()) { // Successful response (HTTP 2xx)
				return response;
			} else {
                String errorResponse = response.getBody(); // Get the error response body
                return createCustomErrorResponse(statusCode, errorResponse, response.getHeaders());
			}
		} catch (HttpClientErrorException | HttpServerErrorException e) {
            // Handle 4xx & 5xx errors
			LogMessage.log(log, "Got Exception:" + e);
            return createCustomErrorResponse(e.getStatusCode(), e.getResponseBodyAsString(), e.getResponseHeaders());
		} catch (Exception e) {
			LogMessage.logException(log, e);
			e.printStackTrace();
			return null;
		}
	}

	private static ResponseEntity<String> createCustomErrorResponse(
			HttpStatusCode statusCode, String errorResponse, HttpHeaders httpHeaders) {
		if(httpHeaders != null) {
			httpHeaders.setContentType(MediaType.APPLICATION_JSON);
		} else {
			httpHeaders = new HttpHeaders();
			httpHeaders.setContentType(MediaType.APPLICATION_JSON);
		}
		
		ResponseEntity<String> response = new ResponseEntity<>(errorResponse, httpHeaders, statusCode);

		LogMessage.debug(log, "createCustomErrorResponse||response:" + response);
		return response;
	}

	private HttpMethod prepareHttpMethod(HttpMethod methodType) {
		return methodType.valueOf(methodType.name());
//		switch (methodType.name()) {
//		case POST:
//			return HttpMethod.POST;
//		case GET:
//			return HttpMethod.GET;
//		case PATCH:
//			return HttpMethod.PATCH;
//		case PUT:
//			return HttpMethod.PUT;
//		default:
//			LogMessage.log(log, "default httpMethod POST ");
//			return HttpMethod.POST;
//		}
	}

}
