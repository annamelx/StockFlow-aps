package br.ufpb.aps.grupo12.servicos;

import br.ufpb.aps.grupo12.dominio.*;
import br.ufpb.aps.grupo12.persistencia.Repositorio;
import br.ufpb.aps.grupo12.seguranca.ServicoAutenticacao;
import br.ufpb.aps.grupo12.seguranca.Usuario;
import java.util.List;
import java.util.Objects;

/**
 * Cadastros auxiliares: categorias, clientes, fornecedores e atendentes (usuárias).
 * Regras de consistência: IDs únicos e nenhuma exclusão que deixe referência quebrada.
 */
public final class ServicoCadastros {
    private final Repositorio repositorio;
    public ServicoCadastros(Repositorio repositorio) { this.repositorio = Objects.requireNonNull(repositorio); }

    // ---------- Categorias (gerente) ----------
    public List<Categoria> categorias(Perfil perfil) {
        Autorizacao.exigir(perfil, false);
        return repositorio.consultar(e -> List.copyOf(e.categorias.values()));
    }
    public Categoria salvarCategoria(Perfil perfil, Categoria c, boolean nova) {
        Autorizacao.exigir(perfil, true);
        Objects.requireNonNull(c);
        return repositorio.transacionar(e -> {
            if (nova && (e.categorias.containsKey(c.id()) || c.id().equals(Categoria.GERAL)))
                throw new ErroNegocio("Categoria duplicada: " + c.id());
            if (!nova && !e.categorias.containsKey(c.id())) throw new ErroNegocio("Categoria inexistente.");
            e.categorias.put(c.id(), c);
            return c;
        });
    }
    public void excluirCategoria(Perfil perfil, String id) {
        Autorizacao.exigir(perfil, true);
        repositorio.transacionar(e -> {
            if (!e.categorias.containsKey(id)) throw new ErroNegocio("Categoria inexistente.");
            if (e.produtos.values().stream().anyMatch(p -> p.categoriaId().equals(id)))
                throw new ErroNegocio("Categoria em uso por produtos; mova os produtos antes de excluir.");
            e.categorias.remove(id);
            return null;
        });
    }

    // ---------- Clientes (gerente e atendente cadastram; só gerente exclui) ----------
    public List<Cliente> clientes(Perfil perfil) {
        Autorizacao.exigir(perfil, false);
        return repositorio.consultar(e -> List.copyOf(e.clientes.values()));
    }
    public Cliente salvarCliente(Perfil perfil, Cliente c, boolean novo) {
        Autorizacao.exigir(perfil, false);
        Objects.requireNonNull(c);
        return repositorio.transacionar(e -> {
            if (novo && e.clientes.containsKey(c.id())) throw new ErroNegocio("Cliente duplicado: " + c.id());
            if (!novo && !e.clientes.containsKey(c.id())) throw new ErroNegocio("Cliente inexistente.");
            e.clientes.put(c.id(), c);
            return c;
        });
    }
    public void excluirCliente(Perfil perfil, String id) {
        Autorizacao.exigir(perfil, true);
        repositorio.transacionar(e -> {
            if (!e.clientes.containsKey(id)) throw new ErroNegocio("Cliente inexistente.");
            if (e.vendas.values().stream().anyMatch(v -> v.clienteId().equals(id)))
                throw new ErroNegocio("Cliente possui vendas registradas e não pode ser excluído.");
            e.clientes.remove(id);
            return null;
        });
    }
    /** Próximo identificador livre com prefixo (ex.: CLI-004, V12). Conveniência para as telas. */
    public String proximoId(Perfil perfil, String prefixo) {
        Autorizacao.exigir(perfil, false);
        return repositorio.consultar(e -> {
            java.util.Set<String> usados = new java.util.HashSet<>();
            switch (prefixo) {
                case "CLI" -> usados.addAll(e.clientes.keySet());
                case "FOR" -> usados.addAll(e.fornecedores.keySet());
                case "V" -> usados.addAll(e.vendas.keySet());
                case "D" -> usados.addAll(e.devolucoes.keySet());
                case "CT" -> usados.addAll(e.contas.keySet());
                case "REP" -> e.movimentacoes.values().forEach(m -> usados.add(m.operacaoId()));
                default -> usados.addAll(e.produtos.keySet());
            }
            boolean curto = prefixo.length() == 1;
            int n = usados.size() + 1;
            String id;
            do { id = curto ? prefixo + n : prefixo + "-" + String.format("%03d", n); n++; }
            while (usados.contains(id));
            return id;
        });
    }

    // ---------- Fornecedores (gerente) ----------
    public List<Fornecedor> fornecedores(Perfil perfil) {
        Autorizacao.exigir(perfil, true);
        return repositorio.consultar(e -> List.copyOf(e.fornecedores.values()));
    }
    public Fornecedor salvarFornecedor(Perfil perfil, Fornecedor f, boolean novo) {
        Autorizacao.exigir(perfil, true);
        Objects.requireNonNull(f);
        return repositorio.transacionar(e -> {
            if (novo && e.fornecedores.containsKey(f.id())) throw new ErroNegocio("Fornecedor duplicado: " + f.id());
            if (!novo && !e.fornecedores.containsKey(f.id())) throw new ErroNegocio("Fornecedor inexistente.");
            e.fornecedores.put(f.id(), f);
            return f;
        });
    }
    public void excluirFornecedor(Perfil perfil, String id) {
        Autorizacao.exigir(perfil, true);
        repositorio.transacionar(e -> {
            if (!e.fornecedores.containsKey(id)) throw new ErroNegocio("Fornecedor inexistente.");
            if (e.compras.values().stream().anyMatch(c -> c.fornecedorId().equals(id)))
                throw new ErroNegocio("Fornecedor possui compras registradas e não pode ser excluído.");
            e.fornecedores.remove(id);
            return null;
        });
    }

    // ---------- Atendentes / usuárias (gerente) ----------
    public List<Usuario> usuarios(Perfil perfil) {
        Autorizacao.exigir(perfil, true);
        return repositorio.consultar(e -> List.copyOf(e.usuarios.values()));
    }
    public Usuario cadastrarUsuario(Perfil perfil, String email, String nome, Perfil novoPerfil, String senha) {
        Autorizacao.exigir(perfil, true);
        if (senha == null || senha.length() < 6) throw new ErroNegocio("A senha deve ter pelo menos 6 caracteres.");
        Usuario u = new Usuario(email, nome, novoPerfil, ServicoAutenticacao.hash(email, senha));
        return repositorio.transacionar(e -> {
            if (e.usuarios.containsKey(u.email())) throw new ErroNegocio("Já existe usuária com este e-mail.");
            e.usuarios.put(u.email(), u);
            return u;
        });
    }
    /** Ativa/desativa acesso. Nunca deixa o sistema sem nenhuma gerente ativa. */
    public Usuario alterarAtivo(Perfil perfil, String email, boolean ativo) {
        Autorizacao.exigir(perfil, true);
        return repositorio.transacionar(e -> {
            Usuario u = e.usuarios.get(email == null ? "" : email.trim().toLowerCase());
            if (u == null) throw new ErroNegocio("Usuária inexistente.");
            if (!ativo && u.perfil() == Perfil.GERENTE && e.usuarios.values().stream()
                .filter(x -> x.perfil() == Perfil.GERENTE && x.ativo()).count() <= 1)
                throw new ErroNegocio("Não é possível desativar a única gerente ativa.");
            Usuario novo = u.comAtivo(ativo);
            e.usuarios.put(novo.email(), novo);
            return novo;
        });
    }
}
