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
import ru.rbpo.lab1.procurement.ProcurementModels.User;
import ru.rbpo.lab1.procurement.ProcurementModels.UserInput;
@RestController
@RequestMapping("/api/users")
public class UsersController {
    private final ProcurementService service;
    public UsersController(ProcurementService service) {
        this.service = java.util.Objects.requireNonNull(service);
    }
    @GetMapping
    public List<User> list() {
        return service.listUsers();
    }
    @GetMapping("/{id}")
    public User get(@PathVariable long id) {
        return service.getUser(id);
    }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public User create(@RequestBody UserInput input) {
        return service.createUser(input);
    }
    @PatchMapping("/{id}")
    public User update(@PathVariable long id, @RequestBody UserInput input) {
        return service.updateUser(id, input);
    }
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        service.deleteUser(id);
    }
}
