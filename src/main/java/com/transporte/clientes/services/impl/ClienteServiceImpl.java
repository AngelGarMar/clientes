package com.transporte.clientes.services.impl;

import com.transporte.clientes.constants.Constants;
import com.transporte.clientes.dto.ClienteRequest;
import com.transporte.clientes.dto.ClienteResponse;
import com.transporte.clientes.dto.ClienteUpdateRequest;
import com.transporte.clientes.entities.Cliente;
import com.transporte.clientes.entities.Puntos;
import com.transporte.clientes.models.ResponseService;
import com.transporte.clientes.repositories.ClienteRepository;
import com.transporte.clientes.repositories.PuntosRepository;
import com.transporte.clientes.services.ClienteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
@Slf4j
//@RefreshScope
public class ClienteServiceImpl implements ClienteService {
    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private PuntosRepository puntosRepository;

    @Value("${app.maintenance.createClient}")
    private boolean createClient;

    @Value("${app.upload.dir}")
    private String directorioUpload;

    private static final List<String> TIPOS_PERMITIDOS = Arrays.asList("image/jpg", "image/jpeg", "image/png", "image/gif");

    @Override
    @Transactional
    public ResponseService createClient(ClienteRequest request, MultipartFile foto) {
        log.info("se revisa si por configuración se puede crear el cliente");
        if (!createClient) {
            log.info("no se puede crear el cliente");
            return new ResponseService(Constants.RESPONSE_TYPE_ERROR, Constants.CLIENT_CREATE_DISABLED, null);
        }
        log.info("se revisa que no exista el número de celular");
        Optional<Cliente> optionalCliente = clienteRepository.findByCelularAndEstatus(request.getCelular(), 1);
        if (optionalCliente.isPresent()) {
            log.info("ya existe un cliente con ese celular");
            return new ResponseService(Constants.RESPONSE_TYPE_ERROR, Constants.CLIENT_CEL_EXISTS, null);
        }
        String image = "";
        if (foto != null && !foto.isEmpty()) {
            boolean fotoValida = true;
            log.info("se comienza a validar el Content-Type de la foto");
            String tipoContenido = foto.getContentType();
            if (tipoContenido == null || !TIPOS_PERMITIDOS.contains(tipoContenido.toLowerCase())) {
                fotoValida = false;
                log.info("la foto no es una imagen válida, no se guardará");
            }
            log.info("se comienza a validar el nombre del archivo");
            String nombreOriginal = foto.getOriginalFilename();
            if (nombreOriginal == null || !nombreOriginal.contains(".")) {
                fotoValida = false;
                log.info("el nombre del archivo es inválido, no se guardará");
            }
            log.info("se comienza a validar la extensión del archivo");
            String extension = nombreOriginal.substring(nombreOriginal.lastIndexOf(".")).toLowerCase();
            List<String> extensionesPermitidas = Arrays.asList(".jpg", ".jpeg", ".png", ".gif");
            if (!extensionesPermitidas.contains(extension)) {
                fotoValida = false;
                log.info("la extensión del archivo no es permitida, no se guardará");
            }
            if (fotoValida) {
                log.info("se comienza a guardar la foto");
                String uniqueFilename = UUID.randomUUID() + "_" + foto.getOriginalFilename();
                Path rootPath = Paths.get(directorioUpload).resolve(uniqueFilename);
                Path rootAbsoluthPath = rootPath.toAbsolutePath();
                log.info("rootAbsoluthPath: {}", rootAbsoluthPath);
                try {
                    if (!Files.exists(Paths.get(directorioUpload))) {
                        log.info("se crea el directorio para guardar las fotos");
                        Files.createDirectories(Paths.get(directorioUpload));
                    }
                    Files.copy(foto.getInputStream(), rootAbsoluthPath);
                    image = uniqueFilename;
                    log.info("foto subida correctamente");
                } catch (IOException e) {
                    log.info("ocurrió un error al guardar la foto: {}", e.getMessage());
                }
            }
        }
        log.info("se guarda en la base de datos");
        Cliente clienteSaved = Cliente.builder()
                .nombre(request.getNombre().trim())
                .apaterno(request.getApaterno() == null ? "" : request.getApaterno().trim())
                .amaterno(request.getAmaterno() == null ? "" : request.getAmaterno().trim())
                .edad(request.getEdad())
                .celular(request.getCelular())
                .tarjeta(request.getTarjeta() == null ? "" : request.getTarjeta().trim())
                .insen(request.getInsen() == null ? "" : request.getInsen().trim())
                .foto(image)
                .creado(Timestamp.valueOf(LocalDateTime.now(ZoneId.of("America/Mexico_City"))))
                .estatus(1)
                .build();
        ClienteResponse clienteResponse = new ClienteResponse(clienteRepository.save(clienteSaved), 0);
        log.info("se regresa el cliente guardado");
        return new ResponseService(Constants.RESPONSE_TYPE_SUCCESS, Constants.CLIENT_SAVED, clienteResponse);
    }

    @Override
    @Transactional
    public ResponseService deleteImage(Integer idCliente) {
        log.info("se busca que exista el cliente");
        Optional<Cliente> clienteOptional = clienteRepository.findByIdAndEstatus(idCliente, 1);
        if (clienteOptional.isEmpty()) {
            log.info("no existe el cliente");
            return new ResponseService(Constants.RESPONSE_TYPE_SUCCESS, Constants.CLIENT_UNKNOWN, null);
        }
        Cliente cliente = clienteOptional.get();
        if (cliente.getFoto().trim().isEmpty()) {
            log.info("el cliente no tiene foto asignada");
            return new ResponseService(Constants.RESPONSE_TYPE_SUCCESS, Constants.CLIENT_SAVED, null);
        }
        log.info("se comienza a eliminar la imagen de la carpeta");
        String foto = cliente.getFoto().trim();
        Path rutaArchivo = Paths.get(directorioUpload).resolve(foto).normalize();
        boolean eliminado;
        try {
            eliminado = Files.deleteIfExists(rutaArchivo);
        } catch (IOException e) {
            log.info("debido a un error, no se pudo eliminar la imagen: {}", e.getMessage());
            return new ResponseService(Constants.RESPONSE_TYPE_ERROR, Constants.IMAGE_NOT_UPDATE, null);
        }
        if (!eliminado) {
            log.info("no se pudo eliminar la imagen");
            return new ResponseService(Constants.RESPONSE_TYPE_ERROR, Constants.IMAGE_NOT_UPDATE, null);
        }
        log.info("se comienza a eliminar el nombre de la imagen de la base de datos");
        cliente.setFoto("");
        cliente.setActualizado(Timestamp.valueOf(LocalDateTime.now(ZoneId.of("America/Mexico_City"))));
        clienteRepository.save(cliente);
        log.info("se regresa el cliente modificado");
        return new ResponseService(Constants.RESPONSE_TYPE_SUCCESS, Constants.CLIENT_SAVED, null);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseService getClient(Integer idCliente) {
        log.info("se busca que exista el cliente");
        Optional<Cliente> clienteOptional = clienteRepository.findByIdAndEstatus(idCliente, 1);
        if (clienteOptional.isEmpty()) {
            log.info("no existe el cliente");
            return new ResponseService(Constants.RESPONSE_TYPE_ERROR, Constants.CLIENT_UNKNOWN, null);
        }
        Cliente cliente = clienteOptional.get();
        ClienteResponse clienteResponse = new ClienteResponse(cliente, getPuntos(idCliente));
        log.info("se regresa el cliente");
        return new ResponseService(Constants.RESPONSE_TYPE_SUCCESS, Constants.CLIENT_GET, clienteResponse);
    }

    @Override
    public Resource getFoto(String foto) {
        log.info("se comienza a obtener la imagen");
        Path rutaArchivo = Paths.get(directorioUpload).resolve(foto).normalize();
        try {
            Resource recurso = new UrlResource(rutaArchivo.toUri());
            log.info("se valida que el archivo exista y sea legible");
            if (recurso.exists() && recurso.isReadable()) {
                log.info("se regresa la foto");
                return recurso;
            } else {
                log.info("la foto no existe o no se puede leer: {}", rutaArchivo);
                return getSinFoto();
            }
        } catch (MalformedURLException e) {
            log.info("debido a un error, no se pudo obtener la foto: {}", e.getMessage());
            return getSinFoto();
        }
    }

    @Override
    @Transactional
    public ResponseService deleteClient(Integer idCliente) {
        log.info("se busca que exista el cliente");
        Optional<Cliente> clienteOptional = clienteRepository.findByIdAndEstatus(idCliente, 1);
        if (clienteOptional.isEmpty()) {
            log.info("no existe el cliente");
            return new ResponseService(Constants.RESPONSE_TYPE_SUCCESS, Constants.CLIENT_UNKNOWN, null);
        }
        Cliente cliente = clienteOptional.get();
        log.info("se comienza a eliminar el cliente");
        cliente.setEstatus(0);
        cliente.setActualizado(Timestamp.valueOf(LocalDateTime.now(ZoneId.of("America/Mexico_City"))));
        clienteRepository.save(cliente);
        log.info("cliente eliminado correctamente");
        return new ResponseService(Constants.RESPONSE_TYPE_SUCCESS, Constants.CLIENT_ELIMINATED, null);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseService getClients() {
        log.info("se comienza a extraer los clientes");
        List<Cliente> clientes = clienteRepository.findAllByEstatus(1);
        List<ClienteResponse> clientesResponse = new ArrayList<>();
        clientes.forEach(cliente -> {
            clientesResponse.add(new ClienteResponse(cliente, getPuntos(cliente.getId())));
        });
        log.info("se regresan los {} clientes", clientes.size());
        return new ResponseService(Constants.RESPONSE_TYPE_SUCCESS, Constants.CLIENTS_GET, clientesResponse);
    }

    @Override
    @Transactional
    public ResponseService updateClient(Integer idCliente, ClienteUpdateRequest request) {
        log.info("se busca que exista el cliente");
        Optional<Cliente> clienteOptional = clienteRepository.findByIdAndEstatus(idCliente, 1);
        if (clienteOptional.isEmpty()) {
            log.info("no existe el cliente");
            return new ResponseService(Constants.RESPONSE_TYPE_SUCCESS, Constants.CLIENT_UNKNOWN, null);
        }
        log.info("se busca que no exista el celular dado en los demás clientes");
        Optional<Cliente> clienteCelular = clienteRepository.findByCelularAndIdNotAndEstatus(request.getCelular().trim(), idCliente, 1);
        if (clienteCelular.isPresent()) {
            log.info("ya existe otro cliente con ese celular, no se puede modificar");
            return new ResponseService(Constants.RESPONSE_TYPE_SUCCESS, Constants.CEL_EXISTS, null);
        }
        Cliente cliente = clienteOptional.get();
        log.info("se comienza a sobreescribir los nuevos datos");
        cliente.setNombre(request.getNombre().trim());
        cliente.setApaterno(request.getApaterno() == null ? "" : request.getApaterno().trim());
        cliente.setAmaterno(request.getAmaterno() == null ? "" : request.getAmaterno().trim());
        cliente.setEdad(request.getEdad());
        cliente.setCelular(request.getCelular());
        cliente.setActualizado(Timestamp.valueOf(LocalDateTime.now(ZoneId.of("America/Mexico_City"))));
        log.info("se guardan los nuevos datos");
        Cliente clienteSaved = clienteRepository.save(cliente);
        ClienteResponse response = new ClienteResponse(clienteSaved, getPuntos(cliente.getId()));
        return new ResponseService(Constants.RESPONSE_TYPE_SUCCESS, Constants.CLIENT_UPDATED, response);
    }

    private Resource getSinFoto() {
        log.info("se regresa la imagen por defecto");
        try {
            Path rutaDefecto = Paths.get(directorioUpload).resolve("sinfoto.jpg");
            return new UrlResource(rutaDefecto.toUri());
        } catch (MalformedURLException e) {
            log.info("no se pudo obtener la imagen por defecto");
            return null;
        }
    }

    private int getPuntos(Integer idClient) {
        Optional<Puntos> puntosOptional = puntosRepository.findTopByOrderByIdDesc(idClient);
        int puntosTotales = 0;
        if (puntosOptional.isPresent()) puntosTotales = puntosOptional.get().getPuntos();
        return puntosTotales;
    }
}
