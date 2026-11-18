package com.transporte.clientes.services;

import com.transporte.clientes.dto.ClienteRequest;
import com.transporte.clientes.dto.ClienteUpdateRequest;
import com.transporte.clientes.models.ResponseService;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface ClienteService {
    ResponseService createClient(ClienteRequest request, MultipartFile foto);
    ResponseService deleteImage(Integer idCliente);
    ResponseService getClient(Integer idCliente);
    Resource getFoto(String foto);
    ResponseService deleteClient(Integer idClient);
    ResponseService getClients();
    ResponseService updateClient(Integer idCliente, ClienteUpdateRequest request);
}
