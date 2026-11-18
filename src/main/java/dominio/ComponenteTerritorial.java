package dominio;

import excecao.DadosInvalidosException;

/**
 * Qualquer equipamento instalado no território (estação de monitoramento, e no
 * futuro comportas, bombas, reservatórios). Guarda o que todos têm em comum
 * (id, nome, região e status) e deixa para cada tipo concreto a forma de
 * conferir a própria situação.
 *
 * O território é descrito por região e equipamento, nunca por pessoas.
 */
public abstract class ComponenteTerritorial {

    private final String id;
    private final String nome;
    private final String regiao;
    private StatusComponente status;

    protected ComponenteTerritorial(String id, String nome, String regiao) {
        this.id = exigirTexto(id, "id");
        this.nome = exigirTexto(nome, "nome");
        this.regiao = exigirTexto(regiao, "região");
        this.status = StatusComponente.NAO_VERIFICADO;
    }

    private static String exigirTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new DadosInvalidosException("componente territorial sem " + campo);
        }
        return valor.trim();
    }

    /**
     * Confere a situação atual do componente, atualiza o status e o devolve.
     * Cada tipo de componente tem o próprio critério.
     */
    public abstract StatusComponente verificarStatus();

    public String getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getRegiao() {
        return regiao;
    }

    public StatusComponente getStatus() {
        return status;
    }

    /** Só as classes filhas alteram o status, e sempre a partir de uma verificação. */
    protected void setStatus(StatusComponente status) {
        this.status = status;
    }
}
