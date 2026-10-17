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
import ru.rbpo.lab1.procurement.ProcurementModels.CommercialOffer;
import ru.rbpo.lab1.procurement.ProcurementModels.CommercialOfferInput;
@RestController
@RequestMapping("/api/commercial-offers")
public class CommercialOffersController {
    private final ProcurementService service;
    public CommercialOffersController(ProcurementService service) {
        this.service = java.util.Objects.requireNonNull(service);
    }
    @GetMapping
    public List<CommercialOffer> list() {
        return service.listCommercialOffers();
    }
    @GetMapping("/{id}")
    public CommercialOffer get(@PathVariable long id) {
        return service.getCommercialOffer(id);
    }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommercialOffer create(@RequestBody CommercialOfferInput input) {
        return service.createCommercialOffer(input);
    }
    @PatchMapping("/{id}")
    public CommercialOffer update(@PathVariable long id, @RequestBody CommercialOfferInput input) {
        return service.updateCommercialOffer(id, input);
    }
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        service.deleteCommercialOffer(id);
    }
}
