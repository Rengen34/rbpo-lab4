package ru.rbpo.lab1.procurement;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.rbpo.lab1.procurement.ProcurementModels.PurchaseOrder;
import ru.rbpo.lab1.procurement.ProcurementModels.PurchaseRequest;
import ru.rbpo.lab1.procurement.ProcurementModels.Supplier;

@RestController
@RequestMapping("/api/business")
public class BusinessOperationsController {
    private final ProcurementService service;

    public BusinessOperationsController(ProcurementService service) {
        this.service = service;
    }

    @PostMapping("/purchase-requests/{id}/publish")
    @PreAuthorize("@procurementAccess.canManageRequest(#id, authentication)")
    public PurchaseRequest publish(@PathVariable long id) {
        return service.publishPurchaseRequest(id);
    }

    @PostMapping("/purchase-requests/{requestId}/select-offer/{offerId}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<PurchaseOrder> selectOffer(
            @PathVariable long requestId,
            @PathVariable long offerId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.selectOffer(requestId, offerId));
    }

    @PostMapping("/purchase-orders/{id}/complete")
    @PreAuthorize("hasRole('MANAGER')")
    public PurchaseOrder complete(@PathVariable long id) {
        return service.completeOrder(id);
    }

    @PostMapping("/purchase-orders/{id}/cancel")
    @PreAuthorize("hasRole('MANAGER')")
    public PurchaseOrder cancel(@PathVariable long id) {
        return service.cancelOrder(id);
    }

    @PostMapping("/suppliers/{id}/suspend")
    @PreAuthorize("hasRole('MANAGER')")
    public Supplier suspend(@PathVariable long id) {
        return service.suspendSupplier(id);
    }
}
