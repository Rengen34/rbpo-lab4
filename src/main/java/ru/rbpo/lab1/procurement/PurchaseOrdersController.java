package ru.rbpo.lab1.procurement;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.rbpo.lab1.procurement.ProcurementModels.PurchaseOrder;
import ru.rbpo.lab1.procurement.ProcurementModels.PurchaseOrderInput;
@RestController
@RequestMapping("/api/purchase-orders")
public class PurchaseOrdersController {
    private final ProcurementService service;
    public PurchaseOrdersController(ProcurementService service) {
        this.service = java.util.Objects.requireNonNull(service);
    }
    @GetMapping
    public List<PurchaseOrder> list() {
        return service.listPurchaseOrders();
    }
    @GetMapping("/{id}")
    public PurchaseOrder get(@PathVariable long id) {
        return service.getPurchaseOrder(id);
    }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PurchaseOrder create(@RequestBody PurchaseOrderInput input) {
        return service.createPurchaseOrder(input);
    }
    @PatchMapping("/{id}")
    public PurchaseOrder update(@PathVariable long id, @RequestBody PurchaseOrderInput input) {
        return service.updatePurchaseOrder(id, input);
    }
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        service.deletePurchaseOrder(id);
    }
}
