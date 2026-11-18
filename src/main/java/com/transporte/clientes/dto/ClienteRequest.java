package com.transporte.clientes.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.io.Serializable;

@Data
public class ClienteRequest implements Serializable {
    @NotBlank(message = "El campo nombre no puede ser vacío.")
    @Size(min = 1, max = 20)
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
    @Size(min = 0, max = 20)
    private String tarjeta;
    @Size(min = 0, max = 45)
    private String insen;
}
