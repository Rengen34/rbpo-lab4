package ru.rbpo.lab1.procurement;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.rbpo.lab1.procurement.ProcurementModels.PurchaseRequest;
import ru.rbpo.lab1.procurement.ProcurementModels.PurchaseRequestInput;
@RestController
@RequestMapping("/api/purchase-requests")
public class PurchaseRequestsController {
    private final ProcurementService service;
    public PurchaseRequestsController(ProcurementService service) {
        this.service = java.util.Objects.requireNonNull(service);
    }
    @GetMapping
    public List<PurchaseRequest> list() {
        return service.listPurchaseRequests();
    }
    @GetMapping("/{id}")
    public PurchaseRequest get(@PathVariable long id) {
        return service.getPurchaseRequest(id);
    }
    @PostMapping
    @PreAuthorize("@procurementAccess.isCurrentUser(#input.authorId(), authentication)")
    @ResponseStatus(HttpStatus.CREATED)
    public PurchaseRequest create(@RequestBody PurchaseRequestInput input) {
        return service.createPurchaseRequest(input);
    }
    @PatchMapping("/{id}")
    @PreAuthorize("@procurementAccess.canManageRequest(#id, authentication)")
    public PurchaseRequest update(@PathVariable long id, @RequestBody PurchaseRequestInput input) {
        return service.updatePurchaseRequest(id, input);
    }
    @DeleteMapping("/{id}")
    @PreAuthorize("@procurementAccess.canManageRequest(#id, authentication)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        service.deletePurchaseRequest(id);
    }
}
