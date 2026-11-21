package org.example.sistema_canchas_deportivas.application.services;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.example.sistema_canchas_deportivas.application.ports.in.ClienteIn;
import org.example.sistema_canchas_deportivas.application.ports.out.ClienteOut;
import org.example.sistema_canchas_deportivas.domain.models.Cliente;
import org.example.sistema_canchas_deportivas.domain.exception.*;

@Service
@Transactional(readOnly = true)
public class ClienteService implements ClienteIn {
    private final ClienteOut clientes;
    private final org.example.sistema_canchas_deportivas.application.ports.out.UsuarioOut usuarios;
    public ClienteService(ClienteOut clientes, org.example.sistema_canchas_deportivas.application.ports.out.UsuarioOut usuarios) {
        this.clientes = clientes; this.usuarios = usuarios;
    }

    @Override public List<Cliente> listar() { return clientes.listar(); }

    @Override public Cliente buscarPorId(Long id) {
        if (id == null || id <= 0) throw new DatoInvalidoException("El ID debe ser positivo.");
        return clientes.buscarPorId(id).orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado."));
    }

    @Override @Transactional
    public Cliente crear(Cliente datos) {
        Cliente nuevo = validarDatos(datos);
        validarUnicos(nuevo, null);
        return clientes.guardar(nuevo);
    }

    @Override @Transactional
    public Cliente actualizar(Long id, Cliente datos) {
        Cliente actual = buscarPorId(id);
        Cliente valido = validarDatos(datos);
        validarUnicos(valido, id);
        actual.actualizar(valido.getCedula(), valido.getNombres(), valido.getApellidos(), valido.getEmail(), valido.getTelefono());
        return clientes.guardar(actual);
    }

    @Override @Transactional
    public void eliminar(Long id) {
        buscarPorId(id);
        clientes.bloquearPorId(id).orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado."));
        if (usuarios.existePorClienteId(id)) throw new ReglaNegocioException("El cliente tiene una cuenta vinculada; elimina primero esa cuenta.");
        clientes.eliminar(id);
    }

    private Cliente validarDatos(Cliente datos) {
        if (datos == null) throw new DatoInvalidoException("Los datos del cliente son obligatorios.");
        return Cliente.nuevo(datos.getCedula(), datos.getNombres(), datos.getApellidos(), datos.getEmail(), datos.getTelefono());
    }

    private void validarUnicos(Cliente cliente, Long idExcluir) {
        if (clientes.existePorCedula(cliente.getCedula(), idExcluir)) throw new ReglaNegocioException("La cédula ya existe.");
        if (clientes.existePorEmail(cliente.getEmail(), idExcluir)) throw new ReglaNegocioException("El email ya existe.");
    }
}
