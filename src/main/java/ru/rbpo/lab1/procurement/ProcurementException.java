package ru.rbpo.lab1.procurement;
public final class ProcurementException extends RuntimeException {
    public enum Kind {
        INVALID_INPUT,
        NOT_FOUND,
        CONFLICT
    }
    private final Kind kind;
    private ProcurementException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }
    public static ProcurementException invalid(String message) {
        return new ProcurementException(Kind.INVALID_INPUT, message);
    }
    public static ProcurementException missing(String message) {
        return new ProcurementException(Kind.NOT_FOUND, message);
    }
    public static ProcurementException conflict(String message) {
        return new ProcurementException(Kind.CONFLICT, message);
    }
    public Kind kind() {
        return kind;
    }
}
