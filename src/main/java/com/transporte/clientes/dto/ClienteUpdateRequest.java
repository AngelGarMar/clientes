package com.transporte.clientes.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
public class ClienteUpdateRequest implements Serializable {
    @Size(min = 0, max = 20)
    private String nombre;
    @Size(min = 0, max = 20)
    private String apaterno;
    @Size(min = 0, max = 20)
    private String amaterno;
    @NotNull(message = "El campo edad no puede ser vacío.")
    @Min(value = 1, message = "La edad mínima permitida es 1")
    private Integer edad;
    @Pattern(regexp = "^$|^\\d{10}$", message = "El campo celular debe tener exactamente 10 números si se proporciona.")
    private String celular;
}
