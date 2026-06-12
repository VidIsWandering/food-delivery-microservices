package com.fooddelivery.payment.adapter.outbound.gateway;

import com.fooddelivery.payment.domain.model.PaymentMethod;
import com.fooddelivery.payment.domain.model.PaymentResult;
import com.fooddelivery.payment.domain.port.outbound.PaymentGateway;
import com.stripe.Stripe;
import com.stripe.model.Charge;
import com.stripe.param.ChargeCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.UUID;

public class StripePaymentGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(StripePaymentGateway.class);

    public StripePaymentGateway(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Stripe API key is not configured. Calls to charge will fail.");
        }
        Stripe.apiKey = apiKey;
    }

    @Override
    public PaymentResult charge(UUID orderId, BigDecimal amount, PaymentMethod method) {
        log.info("Processing Stripe payment for order {}, amount {} VND, method {}", orderId, amount, method);

        if (method == PaymentMethod.COD) {
            log.info("COD payment method selected. Skipping Stripe transaction and returning success.");
            return new PaymentResult(true, "cod_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12), null);
        }

        try {
            // VND is a zero-decimal currency in Stripe, so standard charge value equals amount directly
            long chargeAmount = amount.longValue();

            ChargeCreateParams params = ChargeCreateParams.builder()
                    .setAmount(chargeAmount)
                    .setCurrency("vnd")
                    .setDescription("Payment for Order " + orderId)
                    .setSource("tok_visa") // Use Stripe's default test Visa token
                    .putMetadata("order_id", orderId.toString())
                    .build();

            Charge charge = Charge.create(params);

            if ("succeeded".equals(charge.getStatus())) {
                log.info("Stripe payment SUCCESS for order {}. Charge ID: {}", orderId, charge.getId());
                return new PaymentResult(true, charge.getId(), null);
            } else {
                log.warn("Stripe payment FAILED for order {}. Status: {}, Code: {}", orderId, charge.getStatus(), charge.getFailureCode());
                return new PaymentResult(false, null, charge.getFailureMessage());
            }

        } catch (Exception e) {
            log.error("Exception during Stripe payment execution for order {}", orderId, e);
            return new PaymentResult(false, null, e.getMessage());
        }
    }
}
