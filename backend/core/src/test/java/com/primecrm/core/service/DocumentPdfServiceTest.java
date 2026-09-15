package com.primecrm.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.primecrm.core.audit.AuditService;
import com.primecrm.core.document.DocumentPdfModel;
import com.primecrm.core.document.DocumentPdfWriter;
import com.primecrm.core.dto.common.CustomerSummaryResponse;
import com.primecrm.core.dto.common.OrderSummaryResponse;
import com.primecrm.core.dto.common.ProductSummaryResponse;
import com.primecrm.core.dto.common.UserSummaryResponse;
import com.primecrm.core.dto.contract.ContractResponse;
import com.primecrm.core.dto.order.OrderItemResponse;
import com.primecrm.core.dto.order.OrderResponse;
import com.primecrm.core.dto.proposal.ProposalItemResponse;
import com.primecrm.core.dto.proposal.ProposalResponse;
import com.primecrm.infra.entity.audit.AuditAction;
import com.primecrm.infra.entity.contract.ContractStatus;
import com.primecrm.infra.entity.order.OrderStatus;
import com.primecrm.infra.entity.proposal.ProposalStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DocumentPdfServiceTest {

    @Mock
    private ProposalService proposalService;
    @Mock
    private ProposalItemService proposalItemService;
    @Mock
    private OrderService orderService;
    @Mock
    private OrderItemService orderItemService;
    @Mock
    private ContractService contractService;
    @Mock
    private DocumentPdfWriter writer;
    @Mock
    private AuditService auditService;

    private DocumentPdfService documentPdfService;

    @BeforeEach
    void setUp() {
        documentPdfService = new DocumentPdfService(proposalService, proposalItemService, orderService,
                orderItemService, contractService, writer, auditService);
        when(writer.write(any(DocumentPdfModel.class))).thenReturn(new byte[]{1, 2, 3});
    }

    @Test
    void proposalPdf_buildsFileNameFromCodeAndAudits() {
        UUID id = UUID.randomUUID();
        when(proposalService.findById(id)).thenReturn(proposal(id));
        when(proposalItemService.list(id)).thenReturn(List.of(proposalItem()));

        DocumentPdfService.GeneratedPdf pdf = documentPdfService.proposalPdf(id);

        assertThat(pdf.fileName()).isEqualTo("PRO-001000.pdf");
        assertThat(pdf.content()).isEqualTo(new byte[]{1, 2, 3});
        verify(auditService).recordChange(eq(AuditAction.EXPORT), eq("Proposta"), eq(id), any());
    }

    @Test
    void orderPdf_includesItemsInTheModel() {
        UUID id = UUID.randomUUID();
        when(orderService.findById(id)).thenReturn(order(id));
        when(orderItemService.list(id)).thenReturn(List.of(orderItem()));

        documentPdfService.orderPdf(id);

        ArgumentCaptor<DocumentPdfModel> captor = ArgumentCaptor.forClass(DocumentPdfModel.class);
        verify(writer).write(captor.capture());
        assertThat(captor.getValue().items()).hasSize(1);
        assertThat(captor.getValue().code()).isEqualTo("PED-001000");
    }

    @Test
    void contractPdf_withoutLinkedOrder_hasNoItems() {
        UUID id = UUID.randomUUID();
        when(contractService.findById(id)).thenReturn(contract(id, null));

        documentPdfService.contractPdf(id);

        ArgumentCaptor<DocumentPdfModel> captor = ArgumentCaptor.forClass(DocumentPdfModel.class);
        verify(writer).write(captor.capture());
        assertThat(captor.getValue().items()).isEmpty();
    }

    @Test
    void contractPdf_withLinkedOrder_pullsItemsFromTheOrder() {
        UUID id = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        when(contractService.findById(id)).thenReturn(contract(id, new OrderSummaryResponse(orderId, "PED-002000")));
        when(orderItemService.list(orderId)).thenReturn(List.of(orderItem()));

        documentPdfService.contractPdf(id);

        ArgumentCaptor<DocumentPdfModel> captor = ArgumentCaptor.forClass(DocumentPdfModel.class);
        verify(writer).write(captor.capture());
        assertThat(captor.getValue().items()).hasSize(1);
    }

    private ProposalResponse proposal(UUID id) {
        return new ProposalResponse(id, "PRO-001000", new CustomerSummaryResponse(UUID.randomUUID(), "CLI-001", "Acme"),
                null, null, new UserSummaryResponse(UUID.randomUUID(), "Ana", "ana@primecrm.com"), ProposalStatus.SENT,
                LocalDate.now(), LocalDate.now().plusDays(30), "Observacao", new BigDecimal("100.00"), false, null,
                null, null);
    }

    private ProposalItemResponse proposalItem() {
        return new ProposalItemResponse(UUID.randomUUID(), new ProductSummaryResponse(UUID.randomUUID(), "PRD-001",
                "Consultoria"), null, BigDecimal.ONE, new BigDecimal("100.00"), BigDecimal.ZERO,
                new BigDecimal("100.00"), 0);
    }

    private OrderResponse order(UUID id) {
        return new OrderResponse(id, "PED-001000", new CustomerSummaryResponse(UUID.randomUUID(), "CLI-001", "Acme"),
                null, null, new UserSummaryResponse(UUID.randomUUID(), "Ana", "ana@primecrm.com"), OrderStatus.CONFIRMED,
                LocalDate.now(), LocalDate.now().plusDays(10), "Observacao", new BigDecimal("100.00"), null, null,
                null);
    }

    private OrderItemResponse orderItem() {
        return new OrderItemResponse(UUID.randomUUID(), new ProductSummaryResponse(UUID.randomUUID(), "PRD-001",
                "Consultoria"), null, BigDecimal.ONE, new BigDecimal("100.00"), BigDecimal.ZERO,
                new BigDecimal("100.00"), 0);
    }

    private ContractResponse contract(UUID id, OrderSummaryResponse order) {
        return new ContractResponse(id, "CTR-001000", new CustomerSummaryResponse(UUID.randomUUID(), "CLI-001", "Acme"),
                order, null, new UserSummaryResponse(UUID.randomUUID(), "Ana", "ana@primecrm.com"), null,
                ContractStatus.ACTIVE, LocalDate.now(), null, false, new BigDecimal("100.00"), "Observacao", false,
                null, null, null);
    }
}
