package ru.rbpo.lab1.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import ru.rbpo.lab1.procurement.ProcurementService;

@Component("procurementAccess")
public class ProcurementAccess {
    private final ProcurementService procurement;

    public ProcurementAccess(ProcurementService procurement) {
        this.procurement = procurement;
    }

    public boolean isCurrentUser(Long userId, Authentication authentication) {
        if (hasManagerRole(authentication)) {
            return true;
        }
        return userId != null
                && authentication.getPrincipal() instanceof AccountPrincipal account
                && account.procurementUserId() == userId;
    }
    public boolean canManageRequest(long requestId, Authentication authentication) {
        if (hasManagerRole(authentication)) {
            return true;
        }
        if (authentication.getPrincipal() instanceof AccountPrincipal account) {
            return procurement.getPurchaseRequest(requestId).authorId() == account.procurementUserId();
        }
        return false;
    }

    public boolean canManageOrder(long orderId, Authentication authentication) {
        long requestId = procurement.getPurchaseOrder(orderId).purchaseRequestId();
        return canManageRequest(requestId, authentication);
    }

    private static boolean hasManagerRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_MANAGER".equals(authority.getAuthority()));
    }
}
