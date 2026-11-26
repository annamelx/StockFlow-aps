package br.ufpb.aps.grupo12.interfacegrafica;

import br.ufpb.aps.grupo12.dominio.Perfil;
import br.ufpb.aps.grupo12.observadores.AlertaEstoqueMinimo;
import br.ufpb.aps.grupo12.politicas.fabricas.CatalogoDescontos;
import br.ufpb.aps.grupo12.seguranca.Usuario;
import br.ufpb.aps.grupo12.servicos.LojaFacade;

/** Dependências que as telas recebem (todas criadas no App). A tela só conhece a Facade. */
record Contexto(LojaFacade facade, CatalogoDescontos descontos, AlertaEstoqueMinimo alerta, Usuario usuario) {
    Perfil perfil() { return usuario.perfil(); }
}
