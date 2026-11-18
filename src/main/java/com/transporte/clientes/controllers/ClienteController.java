package com.transporte.clientes.controllers;

import com.transporte.clientes.constants.Constants;
import com.transporte.clientes.dto.ClienteRequest;
import com.transporte.clientes.dto.ClienteUpdateRequest;
import com.transporte.clientes.models.ResponseService;
import com.transporte.clientes.services.ClienteService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/clients")
@Slf4j
public class ClienteController {
    @Autowired
    private ClienteService service;

    @Value("${app.upload.dir}")
    private String directorioUpload;

    @PostMapping
    public ResponseEntity<ResponseService> createClient(@ModelAttribute @Valid ClienteRequest request, BindingResult result,
                                                        @RequestParam(value = "foto", required = false) MultipartFile foto) {
        if (result.hasFieldErrors()) return validation(result);
        ResponseService response = service.createClient(request, foto);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PatchMapping("/{clientId}/foto")
    public ResponseEntity<ResponseService> deleteImage(@PathVariable Integer clientId) {
        ResponseService response = service.deleteImage(clientId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/{clientId}")
    public ResponseEntity<ResponseService> getClient(@PathVariable Integer clientId) {
        ResponseService response = service.getClient(clientId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/foto/{foto:.+}")
    public ResponseEntity<Resource> getFoto(@PathVariable String foto) {
        Resource response = service.getFoto(foto);
        if (response == null || !response.exists()) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        Path rutaArchivo = Paths.get(directorioUpload).resolve(foto).normalize();
        try {
            log.info("se obtiene el tipo de contenido");
            String tipoContenido = Files.probeContentType(rutaArchivo);
            if (tipoContenido == null) tipoContenido = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            log.info("se regresa la imagen");
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(tipoContenido))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + response.getFilename() + "\"")
                    .body(response);
        } catch (IOException e) {
            log.info("debido a un error, no se pudo obtener la imagen: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{clientId}")
    public ResponseEntity<ResponseService> deleteClient(@PathVariable Integer clientId) {
        ResponseService response = service.deleteClient(clientId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<ResponseService> getClients() {
        ResponseService response = service.getClients();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PutMapping("/{clientId}")
    public ResponseEntity<ResponseService> updateClient(@RequestBody @Valid ClienteUpdateRequest request, BindingResult result, @PathVariable Integer clientId) {
        if (result.hasFieldErrors()) return validation(result);
        ResponseService response = service.updateClient(clientId, request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    private ResponseEntity<ResponseService> validation(BindingResult result) {
        Map<String, String> errors = new HashMap<>();
        result.getFieldErrors().forEach(err -> {
            errors.put(err.getField(), err.getDefaultMessage());
        });
        return new ResponseEntity<>(new ResponseService(Constants.RESPONSE_TYPE_ERROR, Constants.MISSED_INFORMATION, errors), HttpStatus.OK);
    }
}
