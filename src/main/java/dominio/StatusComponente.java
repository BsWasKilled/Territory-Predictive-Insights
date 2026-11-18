package dominio;

/** Situação de um componente do território na última verificação. */
public enum StatusComponente {

    NAO_VERIFICADO("ainda não verificado"),
    OPERACIONAL("operacional"),
    PARCIAL("operando parcialmente"),
    INOPERANTE("fora de operação");

    private final String descricao;

    StatusComponente(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
