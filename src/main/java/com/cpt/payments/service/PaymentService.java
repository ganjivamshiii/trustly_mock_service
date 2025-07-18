package com.cpt.payments.service;

import com.cpt.payments.pojo.request.CoreTrustlyProvider;
import com.cpt.payments.pojo.response.TrustlyCoreResponse;

public interface PaymentService {

	TrustlyCoreResponse initiatePayment(CoreTrustlyProvider trustlyProviderRequest);

	void processPayment(String paymentId, String success);

}
