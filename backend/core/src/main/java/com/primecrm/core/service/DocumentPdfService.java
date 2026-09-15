package com.primecrm.core.service;

import com.primecrm.core.document.DocumentPdfModel;
import com.primecrm.core.document.DocumentPdfModel.DocumentPdfField;
import com.primecrm.core.document.DocumentPdfModel.DocumentPdfItem;
import com.primecrm.core.audit.AuditService;
import com.primecrm.core.document.DocumentPdfWriter;
import com.primecrm.core.dto.contract.ContractResponse;
import com.primecrm.core.dto.order.OrderItemResponse;
import com.primecrm.core.dto.order.OrderResponse;
import com.primecrm.core.dto.proposal.ProposalItemResponse;
import com.primecrm.core.dto.proposal.ProposalResponse;
import com.primecrm.infra.entity.audit.AuditAction;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DocumentPdfService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ProposalService proposalService;
    private final ProposalItemService proposalItemService;
    private final OrderService orderService;
    private final OrderItemService orderItemService;
    private final ContractService contractService;
    private final DocumentPdfWriter writer;
    private final AuditService auditService;

    public record GeneratedPdf(String fileName, byte[] content) {
    }

    @Transactional(readOnly = true)
    public GeneratedPdf proposalPdf(UUID id) {
        ProposalResponse proposal = proposalService.findById(id);
        List<ProposalItemResponse> items = proposalItemService.list(id);

        DocumentPdfModel model = new DocumentPdfModel(
                "Proposta Comercial",
                proposal.code(),
                List.of(
                        new DocumentPdfField("Cliente", proposal.customer() != null ? proposal.customer().name() : null),
                        new DocumentPdfField("Contato", proposal.contact() != null ? proposal.contact().name() : null),
                        new DocumentPdfField("Responsavel", proposal.owner() != null ? proposal.owner().name() : null),
                        new DocumentPdfField("Emissao", formatDate(proposal.issueDate())),
                        new DocumentPdfField("Validade", formatDate(proposal.validUntil())),
                        new DocumentPdfField("Status", proposal.status().name())
                ),
                items.stream().map(item -> new DocumentPdfItem(
                        item.product() != null ? item.product().name() : item.description(),
                        item.quantity(), item.unitPrice(), item.discountPercent(), item.total())).toList(),
                proposal.totalAmount(),
                proposal.notes());

        return export("Proposta", id, model);
    }

    @Transactional(readOnly = true)
    public GeneratedPdf orderPdf(UUID id) {
        OrderResponse order = orderService.findById(id);
        List<OrderItemResponse> items = orderItemService.list(id);

        DocumentPdfModel model = new DocumentPdfModel(
                "Pedido de Venda",
                order.code(),
                List.of(
                        new DocumentPdfField("Cliente", order.customer() != null ? order.customer().name() : null),
                        new DocumentPdfField("Responsavel", order.owner() != null ? order.owner().name() : null),
                        new DocumentPdfField("Data do pedido", formatDate(order.orderDate())),
                        new DocumentPdfField("Previsao de entrega", formatDate(order.deliveryDate())),
                        new DocumentPdfField("Status", order.status().name())
                ),
                items.stream().map(item -> new DocumentPdfItem(
                        item.product() != null ? item.product().name() : item.description(),
                        item.quantity(), item.unitPrice(), item.discountPercent(), item.total())).toList(),
                order.totalAmount(),
                order.notes());

        return export("Pedido", id, model);
    }

    @Transactional(readOnly = true)
    public GeneratedPdf contractPdf(UUID id) {
        ContractResponse contract = contractService.findById(id);
        List<OrderItemResponse> items = contract.order() != null
                ? orderItemService.list(contract.order().id())
                : List.of();

        DocumentPdfModel model = new DocumentPdfModel(
                "Contrato",
                contract.code(),
                List.of(
                        new DocumentPdfField("Cliente", contract.customer() != null ? contract.customer().name() : null),
                        new DocumentPdfField("Responsavel", contract.owner() != null ? contract.owner().name() : null),
                        new DocumentPdfField("Pedido de origem", contract.order() != null ? contract.order().code() : null),
                        new DocumentPdfField("Ciclo de faturamento",
                                contract.billingCycle() != null ? contract.billingCycle().name() : null),
                        new DocumentPdfField("Vigencia", formatDate(contract.startDate()) + " a "
                                + (contract.endDate() != null ? formatDate(contract.endDate()) : "indeterminado")),
                        new DocumentPdfField("Renovacao automatica", contract.autoRenew() ? "Sim" : "Nao"),
                        new DocumentPdfField("Status", contract.status().name())
                ),
                items.stream().map(item -> new DocumentPdfItem(
                        item.product() != null ? item.product().name() : item.description(),
                        item.quantity(), item.unitPrice(), item.discountPercent(), item.total())).toList(),
                contract.recurringAmount(),
                contract.notes());

        return export("Contrato", id, model);
    }

    private GeneratedPdf export(String entityName, UUID id, DocumentPdfModel model) {
        byte[] content = writer.write(model);
        auditService.recordChange(AuditAction.EXPORT, entityName, id, Map.of("format", "PDF", "code", model.code()));
        return new GeneratedPdf(model.code() + ".pdf", content);
    }

    private String formatDate(LocalDate date) {
        return date == null ? null : date.format(DATE_FORMAT);
    }
}
