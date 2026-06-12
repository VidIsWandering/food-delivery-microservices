package com.fooddelivery.payment.adapter.inbound.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fooddelivery.payment.domain.model.Payment;
import com.fooddelivery.payment.domain.model.PaymentMethod;
import com.fooddelivery.payment.domain.model.PaymentStatus;
import com.fooddelivery.payment.domain.port.inbound.RefundPaymentUseCase;
import com.fooddelivery.payment.domain.port.outbound.PaymentRepository;
import com.fooddelivery.payment.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PaymentController.class)
@Import({SecurityConfig.class, com.fooddelivery.payment.config.JacksonConfig.class})
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentRepository paymentRepository;

    @MockBean
    private RefundPaymentUseCase refundPaymentUseCase;

    @Test
    @WithMockUser
    void getPaymentByOrderId_Success() throws Exception {
        UUID orderId = UUID.randomUUID();
        Payment payment = new Payment(UUID.randomUUID(), orderId, UUID.randomUUID(), new BigDecimal("50000.00"),
                "VND", PaymentMethod.CREDIT_CARD, PaymentStatus.SUCCESS, "tx_123", null, null, null);

        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));

        mockMvc.perform(get("/api/v1/payments/{orderId}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.order_id").value(orderId.toString()))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void initiateRefund_Admin_Success() throws Exception {
        UUID paymentId = UUID.randomUUID();
        RefundRequest request = new RefundRequest("Saga cancelled", new BigDecimal("50000.00"));

        mockMvc.perform(post("/api/v1/payments/{id}/refund", paymentId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(refundPaymentUseCase, times(1)).refundPayment(paymentId, request.amount(), request.reason());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void initiateRefund_Customer_Forbidden() throws Exception {
        UUID paymentId = UUID.randomUUID();
        RefundRequest request = new RefundRequest("Customer cancelled", new BigDecimal("50000.00"));

        mockMvc.perform(post("/api/v1/payments/{id}/refund", paymentId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(refundPaymentUseCase, never()).refundPayment(any(), any(), any());
    }
}
