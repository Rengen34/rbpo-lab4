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
import ru.rbpo.lab1.procurement.ProcurementModels.Supplier;
import ru.rbpo.lab1.procurement.ProcurementModels.SupplierInput;
@RestController
@RequestMapping("/api/suppliers")
public class SuppliersController {
    private final ProcurementService service;
    public SuppliersController(ProcurementService service) {
        this.service = java.util.Objects.requireNonNull(service);
    }
    @GetMapping
    public List<Supplier> list() {
        return service.listSuppliers();
    }
    @GetMapping("/{id}")
    public Supplier get(@PathVariable long id) {
        return service.getSupplier(id);
    }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Supplier create(@RequestBody SupplierInput input) {
        return service.createSupplier(input);
    }
    @PatchMapping("/{id}")
    public Supplier update(@PathVariable long id, @RequestBody SupplierInput input) {
        return service.updateSupplier(id, input);
    }
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        service.deleteSupplier(id);
    }
}
