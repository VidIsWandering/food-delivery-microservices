package com.fooddelivery.payment.adapter.inbound.rest;

import com.fooddelivery.payment.common.ApiResponse;
import com.fooddelivery.payment.domain.model.Payment;
import com.fooddelivery.payment.domain.port.inbound.RefundPaymentUseCase;
import com.fooddelivery.payment.domain.port.outbound.PaymentRepository;
import com.fooddelivery.payment.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments", description = "Payment management endpoints")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentRepository paymentRepository;
    private final RefundPaymentUseCase refundPaymentUseCase;

    public PaymentController(PaymentRepository paymentRepository, RefundPaymentUseCase refundPaymentUseCase) {
        this.paymentRepository = paymentRepository;
        this.refundPaymentUseCase = refundPaymentUseCase;
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get payment status for an order", security = @SecurityRequirement(name = "X-User-Id"))
    public ApiResponse<PaymentResponse> getPaymentByOrderId(@PathVariable UUID orderId) {
        log.info("Received request to get payment status for order: {}", orderId);
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("PAYMENT_NOT_FOUND", "Payment not found for order: " + orderId));
        return ApiResponse.ok(new PaymentResponse(payment));
    }

    @PostMapping("/{id}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Manually initiate a refund (Admin only)", security = @SecurityRequirement(name = "X-User-Id"))
    public ApiResponse<Void> initiateRefund(
            @PathVariable UUID id,
            @Valid @RequestBody RefundRequest request) {
        log.info("Admin request to initiate refund for payment: {}, reason: {}, amount: {}", id, request.reason(), request.amount());
        refundPaymentUseCase.refundPayment(id, request.amount(), request.reason());
        return ApiResponse.ok(null);
    }
}
